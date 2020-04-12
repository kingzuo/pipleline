package com.tuna.rtmp.client;

import io.vertx.core.Vertx;

public class ClientTest {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    RtmpContext context = new RtmpContext();
    context.setHost("127.0.0.1");
    context.setPort(1935);
    context.setApp("live");
    context.setName("test1");
    context.setFpad(false);
    context.setFlashver("FMLE/3.0 (compatible; FMSc/1.0)");
    context.setCapabilities(239);
    context.setAudioCodecs(1024);
    context.setVidioCodecs(128);
    context.setVidioFunction(1);
    context.setTcUrl("rtmp://127.0.0.1:1935/live");

    RtmpClient rtmpClient = RtmpClient.create(vertx, context);
    rtmpClient.handshake(v -> {
      if (v.succeeded()) {
        rtmpClient.connect(c -> {
          if (c.succeeded()) {
            rtmpClient.createStream(s -> {
              if (s.succeeded()) {
                rtmpClient.fcPublish(f -> {
                  rtmpClient.publish(p -> {
                  });
                });
              }
            });
          }
        });
      } else {
        v.cause().printStackTrace();
      }
    });
  }
}
