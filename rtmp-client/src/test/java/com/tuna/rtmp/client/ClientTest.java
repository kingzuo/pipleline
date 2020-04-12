package com.tuna.rtmp.client;

import io.vertx.core.Vertx;

public class ClientTest {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    RtmpContext context = new RtmpContext();
    context.setHost("127.0.0.1");
    context.setPort(1935);

    RtmpClient rtmpClient = RtmpClient.create(vertx, context);
    rtmpClient.handshake(v -> {
      if (v.succeeded()) {
        System.out.println("handshake success");
      } else {
        v.cause().printStackTrace();
      }
    });
  }
}
