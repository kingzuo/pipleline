package com.tuna.jtt1078.server;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class JTT1078Test {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        JsonObject config = new JsonObject();
        config.put("rtmpHost", "127.0.0.1");
        config.put("rtmpPort", 1935);
        config.put("host", "0.0.0.0");
        config.put("port", 39102);
        config.put("app", "live");

        DeploymentOptions options = new DeploymentOptions();
        options.setConfig(config);
        vertx.deployVerticle(new JTT1078Server(), options);
    }
}
