package com.tuna.rtmp.client;

import static com.tuna.rtmp.api.Constants.*;

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
import java.util.concurrent.atomic.AtomicLong;

public class RtmpClientImpl implements RtmpClient {

  private Vertx vertx;

  private RtmpContext context;

  private NetClient netClient;
  private NetSocketInternal socket;
  private AtomicLong transId = new AtomicLong(1);

  private int chunkSize = DEFAULT_CHUNK_SIZE;
  private int ackWindowSize = 500000;

  public RtmpClientImpl(Vertx vertx, RtmpContext context) {
    this.vertx = vertx;
    this.context = context;
  }

  @Override
  public void handshake(Handler<AsyncResult<Void>> handler) {
    NetClientOptions options = new NetClientOptions().setConnectTimeout(6000);
    options.setReconnectAttempts(3).setReconnectInterval(1000);
    options.setLogActivity(true);
    options.setTcpNoDelay(true);
    options.setTcpKeepAlive(true);

    netClient = vertx.createNetClient(options);

    Promise<NetSocket> connectPromise = Promise.promise();
    netClient.connect(context.getPort(), context.getHost(), connectPromise);

    connectPromise.future().compose(netSocket -> {
      RtmpClientImpl.this.socket = (NetSocketInternal) netSocket;
      socket.channelHandlerContext().pipeline().addBefore("handler", "logger", new LoggingHandler(LogLevel.INFO));
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
          socket.channelHandlerContext().pipeline().addBefore("handler", "rtmpEncoder", new RtmpEncoder());
          socket.channelHandlerContext().pipeline().addBefore("handler", "rtmpDecoder", new RtmpDecoder());
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

  }

  @Override
  public void connect(Handler<AsyncResult<Void>> handler) {
    socket.writeMessage(MessageBuilder.createConnect(transId.getAndIncrement(), context), handler);
  }

  @Override
  public void acknowledgement(long size, Handler<AsyncResult<Void>> handler) {
    socket.writeMessage(MessageBuilder.createAcknowledgement((int) size), handler);
  }

  @Override
  public void acknowledgementWindowSize(long size, Handler<AsyncResult<Void>> handler) {
    socket.writeMessage(MessageBuilder.createAcknowledgementWindowSize((int) size), handler);
  }

  @Override
  public void createStream(Handler<AsyncResult<Void>> handler) {
    socket.writeMessage(MessageBuilder.createStream(transId.getAndIncrement()), handler);
  }

  @Override
  public void fcPublish(Handler<AsyncResult<Void>> handler) {
    socket.writeMessage(MessageBuilder.createFCPublish(transId.getAndIncrement(), context), handler);
  }

  @Override
  public void publish(Handler<AsyncResult<Void>> handler) {
    socket.writeMessage(MessageBuilder.createPublish(transId.getAndIncrement(), context), handler);
  }

  @Override
  public void sendVideo(ByteBuf payload, Handler<AsyncResult<Void>> handler) {
    socket.writeMessage(MessageBuilder.createVideo(payload, context), handler);
  }

  @Override
  public void sendAudeo(ByteBuf payload, Handler<AsyncResult<Void>> handler) {
    socket.writeMessage(MessageBuilder.createAudeo(payload, context), handler);
  }
}
