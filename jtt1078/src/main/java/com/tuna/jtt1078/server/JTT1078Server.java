package com.tuna.jtt1078.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JTT1078Server extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(JTT1078Server.class);
  private NetServer netServer;
  private ConnectionHandler connectionHandler;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    NetServerOptions options = new NetServerOptions();
    options.setTcpCork(true).setTcpFastOpen(true).setTcpKeepAlive(true).setTcpNoDelay(true).setTcpQuickAck(true);
    options.setHost(config().getString("host"));
    options.setPort(config().getInteger("port"));

    netServer = getVertx().createNetServer(options);

    connectionHandler = new ConnectionHandler(context);
    netServer.connectHandler(connectionHandler);

    netServer.listen(result -> {
      if (result.succeeded()) {
        startPromise.complete();
        logger.info("JTT1078 server bind success:{}", config().toString());
      } else {
        startPromise.fail(result.cause());
        logger.error("JTT1078 server bind failed", result.cause());
      }
    });
  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    connectionHandler.close();
    netServer.close(stopPromise.future());
  }
}
