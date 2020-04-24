package com.tuna.jtt1078.codec;

import com.tuna.jtt1078.domain.JTT1078Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JTT1078Decoder extends ByteToMessageDecoder {

  private static final Logger logger = LoggerFactory.getLogger(JTT1078Decoder.class);

  private static ByteBuf FIX_HEADER = UnpooledByteBufAllocator.DEFAULT.buffer(4);

  static {
    FIX_HEADER.writeInt(0x30316364);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if (in.readableBytes() < 16) {
      return;
    }

//    int idx = ByteBufUtil.indexOf(FIX_HEADER, in);
//    if (idx == -1) {
//      in.skipBytes(in.readableBytes());
//    } else {
//      in.readerIndex(idx);
//    }

    int headerSize = 30;

    int frameType = (in.getByte(in.readerIndex() + 15) >> 4) & 0xF;
    if (frameType == 4) {
      headerSize -= 12;
    }
    if (frameType == 3) {
      headerSize -= 4;
    }

    if (in.readableBytes() < headerSize) {
      return;
    }

    int pcktSize = in.getShort(in.readerIndex() + headerSize - 2);
    if (in.readableBytes() < headerSize + pcktSize) {
      return;
    }

    JTT1078Message message = JTT1078Message.create();

    in.skipBytes(5);
    message.setLoadType(in.readUnsignedByte() & 0x7F);
    message.setSequenceNo(in.readUnsignedShort());
    message.setSimNo(readSimNo(in));
    message.setLogicalNo(in.readUnsignedByte());
    int tmp = in.readUnsignedByte();
    message.setFrameType((tmp >> 4) & 0xF);
    message.setPackType(tmp & 0xF);
    if (frameType != 4) {
      message.setTimestamp(in.readLong());
    }
    if (frameType < 3) {
      message.setPts(in.readUnsignedShort());
      message.setDts(in.readUnsignedShort());
    }
    in.skipBytes(2);

    message.getPayload().writeBytes(in, in.readerIndex(), pcktSize);
    in.skipBytes(pcktSize);
    out.add(message);
  }

  /**
   * 0x013562970712 -> 0d13562970712
   *
   * @param in
   * @return
   */
  protected static long readSimNo(ByteBuf in) {
    long tmp = 0;
    tmp += bcd2value(in.readUnsignedByte());
    tmp *= 100;
    tmp += bcd2value(in.readUnsignedByte());
    tmp *= 100;
    tmp += bcd2value(in.readUnsignedByte());
    tmp *= 100;
    tmp += bcd2value(in.readUnsignedByte());
    tmp *= 100;
    tmp += bcd2value(in.readUnsignedByte());
    tmp *= 100;
    tmp += bcd2value(in.readUnsignedByte());
    return tmp;
  }

  protected static int bcd2value(int bcd) {
    int tmp = (bcd >> 4) & 0xF;
    tmp *= 10;
    tmp += bcd & 0xF;
    return tmp;
  }

  protected static void writeSimNo(long sim, ByteBuf out) {
    // TODO
  }
}
