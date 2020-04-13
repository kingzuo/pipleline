package com.tuna.rtmp.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.Vertx;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;

public class ClientTest {

  public static void main(String[] args) {
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
                            byte[] flvBuf = FileUtils.readFileToByteArray(new File("F:\\2020-04-12_23-18-12.flv"));
                            ByteBuf flv = ByteBufAllocator.DEFAULT.buffer(flvBuf.length);
                            flv.writeBytes(flvBuf);
                            try {
                              int vs = 0;
                              flv.skipBytes(9);
                              while (flv.readableBytes() > 3) {
                                System.out.print("PreviousTagSize:" + flv.readUnsignedInt());
                                if (flv.readableBytes() < 1) {
                                  break;
                                }
                                int tmp = flv.readUnsignedByte();
                                if (tmp == 0x08) {
                                  System.out.print(" TagType:0x08=音频");
                                } else if (tmp == 0x09) {
                                  System.out.print(" TagType:0x09=视频");
                                } else if (tmp == 0x12) {
                                  System.out.print(" TagType:0x12=SCRIPT");
                                } else {
                                  throw new RuntimeException("TagHeader err");
                                }
                                int tagDataSize = flv.readUnsignedMedium();
                                System.out.print(" tagDataSize:" + tagDataSize);
                                int timestamp = flv.readUnsignedMedium();
                                System.out.print(" timestamp:" + timestamp);
                                System.out.print(" ExTimestamp:" + flv.readUnsignedByte());
                                System.out.println(" StreamId:" + flv.readUnsignedMedium());
                                ByteBuf data = flv.slice(flv.readerIndex(), tagDataSize);
                                if (tmp == 0x08) { // 音频
                                  rtmpClient.sendAudeo(vs, data);
                                } else if (tmp == 0x09) { // 视频
                                  rtmpClient.sendVideo(vs, data);
                                } else if (tmp == 0x12) {
                                }
                                flv.skipBytes(tagDataSize);
                                Thread.sleep(RandomUtils.nextInt(25, 40));
                                vs += 40;
                              }
                            } finally {
                              ReferenceCountUtil.safeRelease(flv);
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
