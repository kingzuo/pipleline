package com.tuna.jtt1078.server;

import com.tuna.jtt1078.codec.JTT1078Decoder;
import com.tuna.jtt1078.domain.JTT1078Constants;
import com.tuna.jtt1078.domain.JTT1078Message;
import com.tuna.rtmp.client.RtmpClient;
import com.tuna.rtmp.client.RtmpContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
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

public class SocketConnectionHandlerImpl implements Handler<NetSocket> {

  private static final Logger logger = LoggerFactory.getLogger(SocketConnectionHandlerImpl.class);

  /**
   * Reference to the context of the verticle
   */
  private final Context context;

  private Map<String, ProxyContext> sessionMap = new ConcurrentHashMap<>();

  public SocketConnectionHandlerImpl(Context context) {
    this.context = context;
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
          ProxyContext proxyContext = ProxyContext.create(event, context, message.getSimNo(), result -> {
            if (result.succeeded()) {
              doProxyRtmp(result.result(), message);
            } else {
              // session created failed
              logger.error("RtmpClient create failed", result.cause());
              sessionMap.get(key).recycle();
              sessionMap.remove(key);
              event.close();
            }
          });
          sessionMap.put(event.remoteAddress().toString(), proxyContext);
        }
      }
    });

    socketInternal.exceptionHandler(exception -> {
      logger.error("exceptionHandler", exception);
      sessionMap.get(socketInternal.remoteAddress().toString()).recycle();
      sessionMap.remove(socketInternal.remoteAddress().toString());
    });

    socketInternal.closeHandler(v -> {
      logger.info("Session closed sessionId:{}", sessionMap.get(socketInternal.remoteAddress().toString()).getRtmpContext().getName());
      sessionMap.get(socketInternal.remoteAddress().toString()).recycle();
      sessionMap.remove(socketInternal.remoteAddress().toString());
    });
  }

  private String getSplitType(int value) {
    switch (value) {
      case 0:
        return "原子包";
      case 1:
        return "第一包";
      case 2:
        return "最后包";
      case 3:
        return "中间包";
    }
    return null;
  }

  private void debug(JTT1078Message message) {
    if (message.getFrameType() == 0) {
      logger.info("序号:{} I PTS:{} DTS:{} {}:{} ", message.getSequenceNo(), message.getPts(), message.getDts(), getSplitType(message.getPackType()), ByteBufUtil.hexDump(message.getPayload()));
    } else if (message.getFrameType() == 1) {
      logger.info("序号:{} P PTS:{} DTS:{} {}:{} ", message.getSequenceNo(), message.getPts(), message.getDts(), getSplitType(message.getPackType()), ByteBufUtil.hexDump(message.getPayload()));
    } else if (message.getFrameType() == 2) {
      logger.info("序号:{} B PTS:{} DTS:{} {}:{} ", message.getSequenceNo(), message.getPts(), message.getDts(), getSplitType(message.getPackType()), ByteBufUtil.hexDump(message.getPayload()));
    } else if (message.getFrameType() == 3) {
//      logger.info("序号:{} A PTS:{} DTS:{} {}:{} ", message.getSequenceNo(), message.getPts(), message.getDts(), getSplitType(message.getPackType()), ByteBufUtil.hexDump(message.getPayload()));
    } else if (message.getFrameType() == 3) {
//      logger.info("序号:{} D PTS:{} DTS:{} {}:{} ", message.getSequenceNo(), message.getPts(), message.getDts(), getSplitType(message.getPackType()), ByteBufUtil.hexDump(message.getPayload()));
    }
  }

  protected void doProxyRtmp(ProxyContext context, JTT1078Message message) {
//    debug(message);

    if (message.getFrameType() < 3) {
      if (message.getPackType() == 0) {
        // 原子包，直接转发
        sendViedioData(message.getPts(), message.getDts(), message.getPayload(), context);
      } else if (message.getPackType() == 1) { // 第一包
        context.getRecvQueue().writeBytes(message.getPayload());
      } else if (message.getPackType() == 3) { // 中间包
        context.getRecvQueue().writeBytes(message.getPayload());
      } else if (message.getPackType() == 2) { // 最后一包
        context.getRecvQueue().writeBytes(message.getPayload());
        // 原子包，直接转发
        sendViedioData(message.getPts(), message.getDts(), context.getRecvQueue(), context);
        context.getRecvQueue().clear();
      }
    }
    message.recycle();
  }

  protected void sendViedioData(int pts, int dts, ByteBuf h264, ProxyContext context) {
    context.updatePts(pts);
    int idx1 = ByteBufUtil.indexOf(JTT1078Constants.SPLITER, h264);
    while (idx1 != -1) {
      h264.skipBytes(4);
      int idx2 = ByteBufUtil.indexOf(JTT1078Constants.SPLITER, h264);
      ByteBuf nalu;
      if (idx2 != -1) {
        nalu = h264.slice(idx1 + 4, idx2 - h264.readerIndex());
      } else {
        nalu = h264.slice();
      }
      context.getRtmpClient().sendH264Video(context.getPts(), dts, nalu, context.getRtmpContext());
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

    public static ProxyContext create(NetSocket socket, Context context, long simNo, Handler<AsyncResult<ProxyContext>> handler) {
      ProxyContext proxyContext = new ProxyContext();
      proxyContext.setSocketInternal((NetSocketInternal) socket);
      proxyContext.setRtmpContext(createRtmpContext(context, simNo));
      proxyContext.setRtmpClient(RtmpClient.create(context.owner(), proxyContext.getRtmpContext()));
      proxyContext.getRtmpClient().doAllPublishSteps(result -> {
        if (result.succeeded()) {
          handler.handle(Future.succeededFuture(proxyContext));
        } else {
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
        this.pts += 40;
      }
      this.prePts = pts;
    }

    public void recycle() {
      rtmpClient.close();
      rtmpClient = null;
      rtmpContext = null;
      ReferenceCountUtil.safeRelease(recvQueue);
      socketInternal = null;
    }
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
    rtmpContext.setTcUrl(String.format("rtmp://%s:%d/live", context.config().getString("rtmpHost"), context.config().getInteger("rtmpPort")));
    rtmpContext.setLogActivity(context.config().getBoolean("logActivity", false));
    return rtmpContext;
  }
}
