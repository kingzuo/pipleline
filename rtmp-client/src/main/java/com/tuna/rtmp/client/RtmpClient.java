package com.tuna.rtmp.client;

import io.netty.buffer.ByteBuf;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public interface RtmpClient {

  static RtmpClient create(Vertx vertx, RtmpContext context) {
    return new RtmpClientImpl(vertx, context);
  }

  void handshake(Handler<AsyncResult<RtmpClient>> handler);

  void connect(Handler<AsyncResult<RtmpClient>> handler);

  void createStream(Handler<AsyncResult<RtmpClient>> handler);

  void fcPublish(Handler<AsyncResult<RtmpClient>> handler);

  void publish(Handler<AsyncResult<RtmpClient>> handler);

  void doAllPublishSteps(Handler<AsyncResult<RtmpClient>> handler);

  void sendFlvVideo(int pts, ByteBuf payload, Handler<AsyncResult<Void>> handler);

  void sendFlvAudeo(int pts, ByteBuf payload, Handler<AsyncResult<Void>> handler);

  void sendFlvVideo(int pts, ByteBuf payload);

  void sendFlvAudeo(int pts, ByteBuf payload);

  void sendH264Video(int pts, int dts, ByteBuf payload, RtmpContext context);

  void sendH264Audeo(int pts, int dts, ByteBuf payload, RtmpContext context);

  void close(Handler<AsyncResult<Void>> handler);

  void close();
}
