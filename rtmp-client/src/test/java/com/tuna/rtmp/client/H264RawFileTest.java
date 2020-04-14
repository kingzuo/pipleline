package com.tuna.rtmp.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.vertx.core.Vertx;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class H264RawFileTest {

  public static void main(String[] args) throws Exception {
    Vertx vertx = Vertx.vertx();

    RtmpContext context = new RtmpContext();
    context.setHost("127.0.0.1");
    context.setPort(1935);
    context.setApp("live");
    context.setName("test");
    context.setFpad(false);
    context.setFlashver("FMLE/3.0 (compatible; FMSc/1.0)");
    context.setCapabilities(239);
    context.setAudioCodecs(1024);
    context.setVidioCodecs(128);
    context.setVidioFunction(1);
    context.setTcUrl("rtmp://192.168.1.106:1935/live");
    context.setLogActivity(true);

    RtmpClient rtmpClient = RtmpClient.create(vertx, context);
    rtmpClient.handshake(v -> {
      if (v.succeeded()) {
        rtmpClient.connect(c -> {
          if (c.succeeded()) {
            rtmpClient.createStream(s -> {
              if (s.succeeded()) {
                rtmpClient.fcPublish(f -> {
                  if (f.succeeded()) {
                    rtmpClient.publish(p -> {
                      if (p.succeeded()) {
                        vertx.executeBlocking(future -> {
                          try {
                            String file = "D:\\projects\\Rtp1078\\docs\\720p.h264.raw";
                            byte[] buf = FileUtils.readFileToByteArray(new File(file));

                            ByteBuf split = ByteBufAllocator.DEFAULT.buffer(4);
                            split.writeInt(1);

                            ByteBuf h264 = ByteBufAllocator.DEFAULT.buffer(buf.length);
                            h264.writeBytes(buf);
                            h264.markReaderIndex();

                            int pts = 0, dts = 0;
                            int count = 0;

                            int idx1 = ByteBufUtil.indexOf(split, h264);
                            while (idx1 != -1) {
                              h264.skipBytes(4);
                              int idx2 = ByteBufUtil.indexOf(split, h264);
                              ByteBuf nalu = null;
                              if (idx2 != -1) {
                                nalu = h264.slice(idx1 + 4, idx2 - h264.readerIndex());
                              } else {
                                nalu = h264.slice();
                              }
                              h264.skipBytes(nalu.readableBytes());
                              rtmpClient.sendH264Video(pts, dts, nalu, context);

                              idx1 = ByteBufUtil.indexOf(split, h264);

                              dts += 1000 / 25;
                              pts = dts;
                              if (count++ == 9) {
                                Thread.sleep((long) (1000 * count / 25.0D));
                                count = 0;
                              }
                              if (idx1 == -1) {
                                h264.resetReaderIndex();
                                idx1 = ByteBufUtil.indexOf(split, h264);
                              }
                            }
                          } catch (Exception e) {
                            e.printStackTrace();
                          }
                          future.complete();
                        }, result -> {

                        });
                      }
                    });
                  }
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
