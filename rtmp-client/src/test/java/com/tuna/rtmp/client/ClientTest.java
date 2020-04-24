package com.tuna.rtmp.client;

import com.tuna.rtmp.domain.RtmpContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.Vertx;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;

public class ClientTest {

  public static class Frame {

    private ByteBuf data;
    private int timestamp;
    private int type;

    public Frame(ByteBuf data, int timestamp, int type) {
      this.data = data;
      this.timestamp = timestamp;
      this.type = type;
    }

    public ByteBuf getData() {
      return data;
    }

    public void setData(ByteBuf data) {
      this.data = data;
    }

    public int getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(int timestamp) {
      this.timestamp = timestamp;
    }

    public int getType() {
      return type;
    }

    public void setType(int type) {
      this.type = type;
    }
  }

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
    context.setLogActivity(false);

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
                            byte[] flvBuf = FileUtils.readFileToByteArray(new File("F:\\2020-04-14_09-43-04.flv"));
                            ByteBuf flv = ByteBufAllocator.DEFAULT.buffer(flvBuf.length);
                            flv.writeBytes(flvBuf);
                            try {
                              List<Frame> list = new ArrayList<>();

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
                                  list.add(new Frame(data, timestamp, 0));
//                                  as += 1000/44100d;
                                } else if (tmp == 0x09) { // 视频
                                  list.add(new Frame(data, timestamp, 1));
//                                  vs += 1000/30d;
                                } else if (tmp == 0x12) {
                                }
                                flv.skipBytes(tagDataSize);
                                if (list.size() > 2) {
                                  Frame send = list.remove(0);
                                  if (send.getType() == 0) {
                                    rtmpClient.sendFlvAudeo(send.getTimestamp(), send.getData());
                                  } else {
                                    rtmpClient.sendFlvVideo(send.getTimestamp(), send.getData());
                                  }
                                  long dt = list.get(0).getTimestamp() - send.getTimestamp();
                                  if (dt > 0) {
                                    Thread.sleep(dt);
                                  }
                                }
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
