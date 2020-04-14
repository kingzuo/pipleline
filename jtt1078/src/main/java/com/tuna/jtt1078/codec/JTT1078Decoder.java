package com.tuna.jtt1078.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

public class JTT1078Decoder extends ByteToMessageDecoder {

  private static ByteBuf FIX_HEADER = UnpooledByteBufAllocator.DEFAULT.buffer(4);

  static {
    FIX_HEADER.writeInt(0x30316364);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if (in.readableBytes() < 16) {
      return;
    }


  }
}
