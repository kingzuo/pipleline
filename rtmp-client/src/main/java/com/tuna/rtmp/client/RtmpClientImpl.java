package com.tuna.rtmp.client;

import static com.tuna.rtmp.api.Constants.AMF_CMD_CREATE_STREAM;
import static com.tuna.rtmp.api.Constants.C1C2_LENGTH;
import static com.tuna.rtmp.api.Constants.DEFAULT_CHUNK_SIZE;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_ABORT;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_ACK;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_ACK_SIZE;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_AMF3_CMD;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_AMF3_META;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_AMF3_SHARED;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_AMF_CMD;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_AMF_META;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_AMF_SHARED;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_AUDIO;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_BANDWIDTH;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_CHUNK_SIZE;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_EDGE;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_UNDEFINED;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_USER;
import static com.tuna.rtmp.api.Constants.MSG_TYPE_VIDEO;

import com.tuna.rtmp.api.MessageBuilder;
import com.tuna.rtmp.api.ProtocolUtils;
import com.tuna.rtmp.api.RtmpMessage;
import com.tuna.rtmp.codec.RtmpDecoder;
import com.tuna.rtmp.codec.RtmpEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.NetSocketInternal;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RtmpClientImpl implements RtmpClient {

  private Vertx vertx;

  private RtmpContext context;

  private NetClient netClient;
  private NetSocketInternal socket;
  private AtomicLong transId = new AtomicLong(1);

  private int chunkSize = DEFAULT_CHUNK_SIZE;
  private int ackWindowSize = 500000;

  private Map<Long, Handler<AsyncResult<Void>>> callBackHandler = new ConcurrentHashMap();
  private Map<Long, String> callBackHandlerType = new ConcurrentHashMap();

  public RtmpClientImpl(Vertx vertx, RtmpContext context) {
    this.vertx = vertx;
    this.context = context;
  }

  @Override
  public void handshake(Handler<AsyncResult<Void>> handler) {
    NetClientOptions options = new NetClientOptions().setConnectTimeout(6000);
    options.setReconnectAttempts(3).setReconnectInterval(1000);
    options.setLogActivity(context.isLogActivity());
    options.setTcpNoDelay(true);
    options.setTcpKeepAlive(true);
    options.setTcpFastOpen(true);

    netClient = vertx.createNetClient(options);

    Promise<NetSocket> connectPromise = Promise.promise();
    netClient.connect(context.getPort(), context.getHost(), connectPromise);

    connectPromise.future().compose(netSocket -> {
      RtmpClientImpl.this.socket = (NetSocketInternal) netSocket;
      if (context.isLogActivity()) {
        socket.channelHandlerContext().pipeline().addBefore("handler", "logger", new LoggingHandler(LogLevel.INFO));
      }
      socket.channelHandlerContext().pipeline()
          .addBefore("handler", "handshakeDecoder", new FixedLengthFrameDecoder(1 + C1C2_LENGTH * 2));

      ByteBuf byteBuf = socket.channelHandlerContext().alloc().buffer(1 + C1C2_LENGTH);
      ProtocolUtils.handshakeC0C1(byteBuf);
      socket.write(Buffer.buffer(byteBuf));

      Promise<Void> readS0S1S2Promise = Promise.promise();

      socket.messageHandler(msg -> {
        if (msg instanceof ByteBuf) {
          // write S1 to the server
          ((ByteBuf) msg).skipBytes(1);
          ((ByteBuf) msg).writerIndex(((ByteBuf) msg).readerIndex() + C1C2_LENGTH);
          socket.write(Buffer.buffer((ByteBuf) msg), v -> {
            readS0S1S2Promise.handle(v);
          });
        }
      });
      return readS0S1S2Promise.future();
    }).setHandler(v -> {
      try {
        if (v.succeeded()) {
          socket.channelHandlerContext().pipeline().remove("handshakeDecoder");
          socket.channelHandlerContext().pipeline()
              .addBefore("handler", "rtmpEncoder", new RtmpEncoder(RtmpClientImpl.this));
          socket.channelHandlerContext().pipeline()
              .addBefore("handler", "rtmpDecoder", new RtmpDecoder(RtmpClientImpl.this));
          socket.messageHandler(this::rtmpMessageHandler);
        }
        handler.handle(v);
      } catch (Exception e) {
        handler.handle(Future.failedFuture(e));
      }
    });
  }

  protected void rtmpMessageHandler(Object msg) {
    if (msg instanceof RtmpMessage) {
      RtmpMessage rtmpMessage = (RtmpMessage) msg;
      switch (rtmpMessage.getTypeId()) {
        case MSG_TYPE_CHUNK_SIZE:
          this.chunkSize = rtmpMessage.getPayload().readInt();
          socket.writeMessage(MessageBuilder.createSetChunkSize(chunkSize));
          break;
        case MSG_TYPE_ABORT:
          break;
        case MSG_TYPE_ACK:
          break;
        case MSG_TYPE_USER:
          break;
        case MSG_TYPE_ACK_SIZE:
          socket.writeMessage(MessageBuilder.createAcknowledgementWindowSize(ackWindowSize));
          break;
        case MSG_TYPE_BANDWIDTH:
          socket.writeMessage(MessageBuilder.createSetPeerBandWidth(ackWindowSize));
          break;
        case MSG_TYPE_EDGE:
          break;
        case MSG_TYPE_AUDIO:
          break;
        case MSG_TYPE_VIDEO:
          break;
        case MSG_TYPE_AMF3_META:
          break;
        case MSG_TYPE_AMF3_SHARED:
          break;
        case MSG_TYPE_AMF3_CMD:
          break;
        case MSG_TYPE_AMF_META:
          break;
        case MSG_TYPE_AMF_SHARED:
          break;
        case MSG_TYPE_AMF_CMD:
          handAmfCommandResponse(rtmpMessage);
          break;
        case MSG_TYPE_UNDEFINED:
          break;
      }
      rtmpMessage.recycle();
    }
  }

  protected void handAmfCommandResponse(RtmpMessage rtmpMessage) {
    ProtocolUtils.readAmfString(rtmpMessage.getPayload());
    long tsId = ProtocolUtils.readAmfNumber(rtmpMessage.getPayload());
    if (callBackHandler.containsKey(tsId)) {
      callBackHandler.get(tsId).handle(Future.succeededFuture());
      callBackHandler.remove(tsId);
    }

    // Process rtmp server allocate information.
    if (callBackHandlerType.containsKey(tsId)) {
      if (callBackHandlerType.get(tsId).equals(AMF_CMD_CREATE_STREAM)) {
        ProtocolUtils.readAmfNull(rtmpMessage.getPayload());
        long streamId = ProtocolUtils.readAmfNumber(rtmpMessage.getPayload());
        context.setStreamId((int) streamId);
      }
      callBackHandlerType.remove(tsId);
    }
  }

  @Override
  public void connect(Handler<AsyncResult<Void>> handler) {
    long id = transId.getAndIncrement();
    callBackHandler.put(id, handler);
    socket.writeMessage(MessageBuilder.createConnect(id, context));
    vertx.setTimer(context.getRtimeout(), timerId -> {
      if (callBackHandler.containsKey(id)) {
        callBackHandler.get(id).handle(Future.failedFuture("Request timeout"));
        callBackHandler.remove(id);
      }
    });
  }

  public void acknowledgement(long size, Handler<AsyncResult<Void>> handler) {
    socket.writeMessage(MessageBuilder.createAcknowledgement((int) size), handler);
  }

  public void acknowledgementWindowSize(long size, Handler<AsyncResult<Void>> handler) {
    socket.writeMessage(MessageBuilder.createAcknowledgementWindowSize((int) size), handler);
  }

  @Override
  public void createStream(Handler<AsyncResult<Void>> handler) {
    long id = transId.getAndIncrement();
    callBackHandler.put(id, handler);
    callBackHandlerType.put(id, AMF_CMD_CREATE_STREAM);
    socket.writeMessage(MessageBuilder.createStream(id));
    vertx.setTimer(context.getRtimeout(), timerId -> {
      if (callBackHandler.containsKey(id)) {
        callBackHandler.get(id).handle(Future.failedFuture("Request timeout"));
        callBackHandler.remove(id);
        callBackHandlerType.remove(id);
      }
    });
  }

  @Override
  public void fcPublish(Handler<AsyncResult<Void>> handler) {
    long id = transId.getAndIncrement();
    socket.writeMessage(MessageBuilder.createFCPublish(id, context), handler);
  }

  @Override
  public void publish(Handler<AsyncResult<Void>> handler) {
    long id = transId.getAndIncrement();
    socket.writeMessage(MessageBuilder.createPublish(id, context), handler);
  }

  @Override
  public void sendVideo(int timestamp, ByteBuf payload, Handler<AsyncResult<Void>> handler) {
    socket.writeMessage(MessageBuilder.createVideo(timestamp, payload, context), handler);
  }

  @Override
  public void sendAudeo(int timestamp, ByteBuf payload, Handler<AsyncResult<Void>> handler) {
    socket.writeMessage(MessageBuilder.createAudeo(timestamp, payload, context), handler);
  }

  @Override
  public void sendVideo(int timestamp, ByteBuf payload) {
    socket.writeMessage(MessageBuilder.createVideo(timestamp, payload, context));
  }

  @Override
  public void sendAudeo(int timestamp, ByteBuf payload) {
    socket.writeMessage(MessageBuilder.createAudeo(timestamp, payload, context));
  }

  @Override
  public void close(Handler<AsyncResult<Void>> handler) {

  }

  @Override
  public void close() {
    socket.close();
    netClient.close();
    vertx.close();
  }

  public int getChunkSize() {
    return chunkSize;
  }
}
