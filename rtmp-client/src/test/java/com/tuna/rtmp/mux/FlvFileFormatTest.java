package com.tuna.rtmp.mux;

import static com.tuna.rtmp.domain.Constants.*;

import com.tuna.rtmp.domain.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;

public class FlvFileFormatTest {

  public static void main(String[] args) throws IOException {
    byte[] flvBuf = FileUtils.readFileToByteArray(new File("F:\\2020-04-14_09-43-04.flv"));
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
      System.out.print(" timestamp:" + flv.readUnsignedMedium());
      System.out.print(" ExTimestamp:" + flv.readUnsignedByte());
      System.out.print(" StreamId:" + flv.readUnsignedMedium());
      if (tmp == 0x08) {
        tmp = flv.getUnsignedByte(flv.readerIndex());
        System.out.print(" 音频:SoundFormat:" + ((tmp >> 4) & 0xF));
        System.out.print(" 音频:SoundRate:" + ((tmp >> 2) & 0x3));
        System.out.print(" 音频:SoundSize:" + ((tmp >> 1) & 0x1));
        System.out.println(" 音频:SoundType:" + ((tmp >> 0) & 0x1));
      } else if (tmp == 0x09) {
        System.out.println(" 视频");
      } else if (tmp == 0x12) {
        System.out.println(" SCRIPT:");
        parseScript(flv.slice(flv.readerIndex(), tagDataSize));
      }
      flv.skipBytes(tagDataSize);
    }
  }

  public static void parseScript(ByteBuf byteBuf) {
    while (byteBuf.readableBytes() > 1) {
      int type = byteBuf.getUnsignedByte(byteBuf.readerIndex());
      switch (type) {
        case AMF_TYPE_NUMBER:
          System.out.println(ProtocolUtils.readAmfNumber(byteBuf));
          break;
        case AMF_TYPE_BOOLEAN:
          System.out.println(ProtocolUtils.readAmfBoolean(byteBuf));
          break;
        case AMF_TYPE_STRING:
          System.out.println(ProtocolUtils.readAmfString(byteBuf));
          break;
        case AMF_TYPE_OBJECT:
          System.out.println("OBJECT");
          byteBuf.skipBytes(1);
          break;
        case AMF_TYPE_NULL:
          System.out.println("NULL");
          ProtocolUtils.readAmfNull(byteBuf);
          break;
        case AMF_TYPE_ARRAY_NULL:
          break;
        case AMF_TYPE_MIXED_ARRAY: {
          byteBuf.skipBytes(1);
          long size = byteBuf.readUnsignedInt();
          for (int i = 0; i < size; i++) {
            System.out.println(ProtocolUtils.readString(byteBuf));
            int subType = byteBuf.getUnsignedByte(byteBuf.readerIndex());
            switch (subType) {
              case AMF_TYPE_STRING:
                System.out.println(ProtocolUtils.readAmfString(byteBuf));
                break;
              case AMF_TYPE_NUMBER:
                System.out.println(ProtocolUtils.readAmfNumber(byteBuf));
                break;
              case AMF_TYPE_BOOLEAN:
                System.out.println(ProtocolUtils.readAmfBoolean(byteBuf));
                break;
              default:
                System.out.println("11111");
                break;
            }
          }
          ProtocolUtils.readAmfEnd(byteBuf);
          break;
        }
        case AMF_TYPE_END:
          System.out.println("OBJECT END");
          ProtocolUtils.readAmfEnd(byteBuf);
          break;
        case AMF_TYPE_ARRAY:
          break;
      }
    }
  }
}
