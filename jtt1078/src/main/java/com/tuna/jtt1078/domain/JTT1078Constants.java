package com.tuna.jtt1078.domain;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;

public class JTT1078Constants {

  public static final int LOAD_TYPE_AAC = 19;

  public static final int LOAD_TYPE_H264 = 98;
  public static final int LOAD_TYPE_H265 = 99;
  public static final int LOAD_TYPE_AVS = 100;
  public static final int LOAD_TYPE_SVAC= 101;

  public static final ByteBuf SPLITER = UnpooledByteBufAllocator.DEFAULT.buffer(4);

  static {
    SPLITER.writeInt(1);
  }
}
