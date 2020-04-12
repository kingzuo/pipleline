package com.tuna.rtmp.client;

import static com.tuna.rtmp.api.Constants.C1C2_LENGTH;

import com.tuna.rtmp.api.ProtocolUtils;
import com.tuna.rtmp.codec.RtmpDecoder;
import com.tuna.rtmp.codec.RtmpEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.FixedLengthFrameDecoder;
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

public class RtmpClientImpl implements RtmpClient {

  private Vertx vertx;

  private RtmpContext context;

  private NetClient netClient;
  private NetSocketInternal socket;

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

  }

  @Override
  public void connect(Handler<AsyncResult<Void>> handler) {

  }

  @Override
  public void acknowledgement(long size, Handler<AsyncResult<Void>> handler) {

  }

  @Override
  public void acknowledgementWindowSize(long size, Handler<AsyncResult<Void>> handler) {

  }

  @Override
  public void createStream(Handler<AsyncResult<Void>> handler) {

  }

  @Override
  public void fcPublish(Handler<AsyncResult<Void>> handler) {

  }

  @Override
  public void publish(Handler<AsyncResult<Void>> handler) {

  }

  @Override
  public void sendVideo(ByteBuf payload, Handler<AsyncResult<Void>> handler) {

  }

  @Override
  public void sendAudeo(ByteBuf payload, Handler<AsyncResult<Void>> handler) {

  }
}
