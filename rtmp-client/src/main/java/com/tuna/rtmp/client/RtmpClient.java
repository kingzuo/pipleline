package com.tuna.rtmp.client;

import io.netty.buffer.ByteBuf;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public interface RtmpClient {

  static RtmpClient create(Vertx vertx, RtmpContext context) {
    return new RtmpClientImpl(vertx, context);
  }

  void handshake(Handler<AsyncResult<Void>> handler);

  void connect(Handler<AsyncResult<Void>> handler);

  void createStream(Handler<AsyncResult<Void>> handler);

  void fcPublish(Handler<AsyncResult<Void>> handler);

  void publish(Handler<AsyncResult<Void>> handler);

  void sendVideo(ByteBuf payload, Handler<AsyncResult<Void>> handler);

  void sendAudeo(ByteBuf payload, Handler<AsyncResult<Void>> handler);

  void sendVideo(ByteBuf payload);

  void sendAudeo(ByteBuf payload);

  void close(Handler<AsyncResult<Void>> handler);

  void close();
}
