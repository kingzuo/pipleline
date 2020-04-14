package com.tuna.rtmp.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.nio.charset.Charset;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

public class ProtocolUtils implements Constants {

  public static void handshakeC0C1(ByteBuf byteBuf) {
    byteBuf.writeByte(VERSION);
    byteBuf.writeInt(0);
    byteBuf.writeInt(0);
    for (int i = 0; i < C1C2_LENGTH - 8; i++) {
      byteBuf.writeByte(RandomUtils.nextInt(0, 255));
    }
  }

  public static void writeAmfNumber(ByteBuf byteBuf, long value) {
    byteBuf.writeByte(AMF_TYPE_NUMBER);
    byteBuf.writeDouble(value);
  }

  public static long readAmfNumber(ByteBuf byteBuf) {
    if (byteBuf.readableBytes() < 9) {
      throw new RuntimeException("readAmfNumber length err:" + ByteBufUtil.hexDump(byteBuf));
    }
    if (byteBuf.readUnsignedByte() != AMF_TYPE_NUMBER) {
      throw new RuntimeException("readAmfNumber type err:" + ByteBufUtil.hexDump(byteBuf));
    }
    return (long) byteBuf.readDouble();
  }

  public static void writeAmfBoolean(ByteBuf byteBuf, boolean value) {
    byteBuf.writeByte(AMF_TYPE_BOOLEAN);
    if (value) {
      byteBuf.writeByte(1);
    } else {
      byteBuf.writeByte(0);
    }
  }

  public static boolean readAmfBoolean(ByteBuf byteBuf) {
    if (byteBuf.readableBytes() < 2) {
      throw new RuntimeException("readAmfNumber length err:" + ByteBufUtil.hexDump(byteBuf));
    }
    if (byteBuf.readUnsignedByte() != AMF_TYPE_BOOLEAN) {
      throw new RuntimeException("readAmfNumber type err:" + ByteBufUtil.hexDump(byteBuf));
    }
    int v = byteBuf.readUnsignedByte();
    if (v == 0) {
      return false;
    }
    return true;
  }

  public static void writeAmfString(ByteBuf byteBuf, String msg) {
    byteBuf.writeByte(AMF_TYPE_STRING);
    writeString(byteBuf, msg);
  }

  public static String readAmfString(ByteBuf byteBuf) {
    if (byteBuf.readableBytes() < 3) {
      throw new RuntimeException("readAmfString length err:" + ByteBufUtil.hexDump(byteBuf));
    }
    if (byteBuf.readUnsignedByte() != AMF_TYPE_STRING) {
      throw new RuntimeException("readAmfString type err:" + ByteBufUtil.hexDump(byteBuf));
    }
    return readString(byteBuf);
  }

  public static void writeString(ByteBuf byteBuf, String key) {
    if (StringUtils.isNotEmpty(key)) {
      byteBuf.writeShort(key.length());
      byteBuf.writeCharSequence(key, Charset.forName("UTF-8"));
    } else {
      byteBuf.writeShort(0);
    }
  }

  public static String readString(ByteBuf byteBuf) {
    int size = byteBuf.readUnsignedShort();
    if (byteBuf.readableBytes() < size) {
      throw new RuntimeException("readString length err:" + ByteBufUtil.hexDump(byteBuf));
    }
    return byteBuf.readCharSequence(size, Charset.forName("UTF-8")).toString();
  }

  public static void writeAmfObjectStart(ByteBuf byteBuf) {
    byteBuf.writeByte(AMF_TYPE_OBJECT);
  }

  public static void writeAmfObjectEnd(ByteBuf byteBuf) {
    byteBuf.writeMedium(AMF_TYPE_END);
  }

  public static void writeAmfNull(ByteBuf byteBuf) {
    byteBuf.writeByte(AMF_TYPE_NULL);
  }

  public static void readAmfNull(ByteBuf byteBuf) {
    int type = byteBuf.readUnsignedByte();
    if (type != AMF_TYPE_NULL) {
      throw new RuntimeException("readAmfNull err:" + ByteBufUtil.hexDump(byteBuf));
    }
  }

  public static void readAmfEnd(ByteBuf byteBuf) {
    int type = byteBuf.readUnsignedMedium();
    if (type != AMF_TYPE_END) {
      throw new RuntimeException("readAmfEnd err:" + ByteBufUtil.hexDump(byteBuf));
    }
  }
}
