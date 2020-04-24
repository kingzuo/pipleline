package com.tuna.jtt1078.server;

import com.tuna.jtt1078.codec.JTT1078Decoder;
import com.tuna.jtt1078.domain.JTT1078Message;
import com.tuna.rtmp.client.RtmpClient;
import com.tuna.rtmp.domain.RtmpContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.impl.NetSocketInternal;
import io.vertx.core.net.NetSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionHandler implements Handler<NetSocket> {

  private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);
  public static final ByteBuf SPLITER = UnpooledByteBufAllocator.DEFAULT.buffer(4);

  static {
    SPLITER.writeInt(1);
  }

  /**
   * Reference to the context of the verticle
   */
  private final Context context;

  private Map<String, ProxyContext> sessionMap = new ConcurrentHashMap<>();

  public ConnectionHandler(Context context) {
    this.context = context;
  }

  private void destroySession(NetSocketInternal socketInternal) {
    if (socketInternal != null) {
      String sessionId = socketInternal.remoteAddress().toString();
      if (sessionMap.containsKey(sessionId)) {
        sessionMap.get(sessionId).recycle();
        sessionMap.remove(sessionId);
      }
    }
  }

  @Override
  public void handle(NetSocket event) {
    NetSocketInternal socketInternal = (NetSocketInternal) event;
    socketInternal.channelHandlerContext().pipeline().addBefore("handler", "jtt1078Decoder", new JTT1078Decoder());
    socketInternal.messageHandler(obj -> {
      if (obj instanceof JTT1078Message) {
        final JTT1078Message message = (JTT1078Message) obj;
        String key = event.remoteAddress().toString();
        if (sessionMap.containsKey(key)) {
          doProxyRtmp(sessionMap.get(key), message);
        } else {
          // 创建转发session
          socketInternal.pause();
          ProxyContext.create(event, context, message.getSimNo(), result -> {
            if (result.succeeded()) {
              sessionMap.put(event.remoteAddress().toString(), result.result());
              result.result().setProxyClosedHandler(v -> {
                destroySession(socketInternal);
              });
              socketInternal.resume();
              doProxyRtmp(result.result(), message);
            } else {
              // session created failed
              logger.error("RtmpClient create failed", result.cause());
              destroySession(socketInternal);
              event.close();
            }
          });
        }
      }
    });

    socketInternal.exceptionHandler(exception -> {
      logger.error("exceptionHandler", exception);
      destroySession(socketInternal);
    });

    socketInternal.closeHandler(v -> {
      logger.info("Session closed sessionId:{}", socketInternal.remoteAddress().toString());
      destroySession(socketInternal);
    });
  }

  protected void doProxyRtmp(ProxyContext context, JTT1078Message message) {
    try {
      if (message.getFrameType() < 3) {
        if (message.getPackType() == 0) { // 原子包，直接转发
          sendViedioData(message.getPts(), message.getDts(), message.getPayload(), context);
        } else if (message.getPackType() == 1) { // 第一包
          context.getRecvQueue().writeBytes(message.getPayload());
        } else if (message.getPackType() == 3) { // 中间包
          context.getRecvQueue().writeBytes(message.getPayload());
        } else if (message.getPackType() == 2) { // 最后一包
          context.getRecvQueue().writeBytes(message.getPayload());
          sendViedioData(message.getPts(), message.getDts(), context.getRecvQueue(), context);
          context.getRecvQueue().clear();
        }
      }
    } finally {
      message.recycle();
    }
  }

  protected void sendViedioData(int pts, int dts, ByteBuf h264, ProxyContext context) {
    context.updatePts(pts);
    int idx1 = ByteBufUtil.indexOf(SPLITER, h264);
    while (idx1 != -1) {
      h264.skipBytes(4);
      int idx2 = ByteBufUtil.indexOf(SPLITER, h264);
      ByteBuf nalu;
      if (idx2 != -1) {
        nalu = h264.slice(idx1 + 4, idx2 - h264.readerIndex());
      } else {
        nalu = h264.slice();
      }
      if (nalu.readableBytes() > 0) {
        context.getRtmpClient().sendH264Video(context.getPts(), dts, nalu, context.getRtmpContext());
      }
      h264.skipBytes(nalu.readableBytes());
      idx1 = ByteBufUtil.indexOf(SPLITER, h264);
    }
  }

  public static class ProxyContext {

    private String id;
    private RtmpClient rtmpClient;
    private RtmpContext rtmpContext;
    private ByteBuf recvQueue = ByteBufAllocator.DEFAULT.buffer();
    private NetSocketInternal socketInternal;
    private int pts = 0;
    private int prePts = 0;

    public static ProxyContext create(NetSocket socket, Context context, long simNo,
        Handler<AsyncResult<ProxyContext>> handler) {
      ProxyContext proxyContext = new ProxyContext();
      proxyContext.setSocketInternal((NetSocketInternal) socket);
      proxyContext.setRtmpContext(createRtmpContext(context, simNo));
      proxyContext.setRtmpClient(RtmpClient.create(context.owner(), proxyContext.getRtmpContext()));
      proxyContext.getRtmpClient().doAllPublishSteps(result -> {
        if (result.succeeded()) {
          logger.info("Client sim: {} doAllPublishSteps success", simNo);
          handler.handle(Future.succeededFuture(proxyContext));
        } else {
          logger.warn("Client sim: {} doAllPublishSteps failed ", simNo);
          proxyContext.recycle();
          handler.handle(Future.failedFuture(result.cause()));
        }
      });
      return proxyContext;
    }

    public RtmpClient getRtmpClient() {
      return rtmpClient;
    }

    public void setRtmpClient(RtmpClient rtmpClient) {
      this.rtmpClient = rtmpClient;
    }

    public RtmpContext getRtmpContext() {
      return rtmpContext;
    }

    public void setRtmpContext(RtmpContext rtmpContext) {
      this.rtmpContext = rtmpContext;
    }

    public ByteBuf getRecvQueue() {
      return recvQueue;
    }

    public void setRecvQueue(ByteBuf recvQueue) {
      this.recvQueue = recvQueue;
    }

    public NetSocketInternal getSocketInternal() {
      return socketInternal;
    }

    public void setSocketInternal(NetSocketInternal socketInternal) {
      this.socketInternal = socketInternal;
    }

    public String getId() {
      if (id == null) {
        id = socketInternal.remoteAddress().toString();
      }
      return id;
    }

    public int getPts() {
      return pts;
    }

    public void updatePts(int pts) {
      if (pts - prePts > 0) {
        this.pts += pts - prePts;
      } else {
        this.pts += 1000 / 30;
      }
      this.prePts = pts;
    }

    public void setProxyClosedHandler(Handler<Void> handler) {
      rtmpClient.setCloseHandler(handler);
    }

    public void recycle() {
      rtmpClient.close();
      socketInternal.close();
      rtmpClient = null;
      rtmpContext = null;
      socketInternal = null;
      ReferenceCountUtil.safeRelease(recvQueue);
    }
  }

  public void close() {
    sessionMap.forEach((key, session) -> {
      session.recycle();
    });
    sessionMap.clear();
  }

  protected static RtmpContext createRtmpContext(Context context, long simNo) {
    RtmpContext rtmpContext = new RtmpContext();
    rtmpContext.setHost(context.config().getString("rtmpHost"));
    rtmpContext.setPort(context.config().getInteger("rtmpPort"));
    rtmpContext.setApp(context.config().getString("app"));
    rtmpContext.setName(Long.toString(simNo));
    rtmpContext.setFpad(false);
    rtmpContext.setFlashver("FMLE/3.0 (compatible; FMSc/1.0)");
    rtmpContext.setCapabilities(239);
    rtmpContext.setAudioCodecs(1024);
    rtmpContext.setVidioCodecs(128);
    rtmpContext.setVidioFunction(1);
    rtmpContext.setTcUrl(String
        .format("rtmp://%s:%d/%s", context.config().getString("rtmpHost"), context.config().getInteger("rtmpPort"),
            context.config().getString("app")));
    rtmpContext.setLogActivity(context.config().getBoolean("logActivity", false));
    return rtmpContext;
  }
}
