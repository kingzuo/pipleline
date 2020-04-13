package com.tuna.rtmp.mux;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;

public class FlvFileFormatTest {

  public static void main(String[] args) throws IOException {
    byte[] flvBuf = FileUtils.readFileToByteArray(new File("F:\\2020-04-12_23-18-12.flv"));
    ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(flvBuf.length);
    byteBuf.writeBytes(flvBuf);
    try {
      parseHeader(byteBuf);
      parseFlvTag(byteBuf);
    } finally {
      ReferenceCountUtil.safeRelease(byteBuf);
    }
    System.out.println(Double.longBitsToDouble(0x403E0000_00000000L));
  }

  public static void parseHeader(ByteBuf flv) {
    System.out.println("Header:" + flv.readCharSequence(3, Charset.defaultCharset()));
    System.out.println("Version:" + flv.readUnsignedByte());
    int tmp = flv.readUnsignedByte();
    System.out.println("Video:" + ((tmp >> 2) & 0x1));
    System.out.println("Audeo:" + ((tmp >> 0) & 0x1));
    System.out.println("Head size:" + flv.readUnsignedInt());
  }

  public static void parseFlvTag(ByteBuf flv) {
    while (flv.readableBytes() > 3) {
      System.out.print("PreviousTagSize:" + flv.readUnsignedInt());
      if(flv.readableBytes() < 1)
        break;
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
      System.out.print(" timestamp:" + flv.readUnsignedMedium());
      System.out.print(" ExTimestamp:" + flv.readUnsignedByte());
      System.out.print(" StreamId:" + flv.readUnsignedMedium());
      if (tmp == 0x08) {
        tmp = flv.getUnsignedByte(flv.readerIndex());
        System.out.print(" 音频:SoundFormat:" + ((tmp >> 4)&0xF));
        System.out.print(" 音频:SoundRate:" + ((tmp >> 2)&0x3));
        System.out.print(" 音频:SoundSize:" + ((tmp >> 1)&0x1));
        System.out.println(" 音频:SoundType:" + ((tmp >> 0)&0x1));
      } else if (tmp == 0x09) {
        System.out.println(" 视频");
      } else if (tmp == 0x12) {
        System.out.println(" SCRIPT:");
      }
      flv.skipBytes(tagDataSize);
    }
  }
}
