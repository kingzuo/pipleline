package com.tuna.rtmp.codec;

import com.tuna.rtmp.api.Constants;
import com.tuna.rtmp.api.RtmpMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ReferenceCountUtil;
import java.util.List;

/**
 * RTMP chunk format
 *
 * <p>  0              1               2                  3</p>
 * <p>+--------------+----------------+------------------+------------+</p>
 * <p>| Basic Header | Message Header | Extend Timestamp | Chunk Data |</p>
 * <p>+--------------+----------------+------------------+------------+</p>
 * <p>|<---------------- Chunk Header ------------------>|</p>
 *
 *
 * <p>fmt = 0  11 bytes</p>
 * <p>     0              1               2                  3</p>
 * <p>+-----------+--------------+----------------+-------------------+</p>
 * <p>|               timestamp                   |   message length  |</p>
 * <p>+-----------+--------------+----------------+-------------------+</p>
 * <p>|   message length (cont)  |message type id |   msg stream id   |</p>
 * <p>+-----------+--------------+----------------+-------------------+</p>
 * <p>|       message stream id (cont)            |</p>
 * <p>+-----------+--------------+----------------+-------------------+</p>
 *
 * <p>fmt = 1  7 bytes<p/>
 * <p>     0              1               2                  3<p/>
 * <p>+-----------+--------------+----------------+-------------------+<p/>
 * <p>|               timestamp                   |   message length  |<p/>
 * <p>+-----------+--------------+----------------+-------------------+<p/>
 * <p>|   message length (cont)  |message type id |<p/>
 * <p>+-----------+--------------+----------------+-------------------+<p/>
 *
 * <p>fmt = 2  3 bytes</p>
 * <p>     0              1               2                  3</p>
 * <p>+-----------+--------------+----------------+-------------------+</p>
 * <p>|               timestamp                   |</p>
 * <p>+-----------+--------------+----------------+-------------------+</p>
 *
 * <p>fmt = 3  0 bytes</p>
 *
 * Now we just decode the fmt=0 and fmt=3 split case.
 */
public class RtmpDecoder extends ByteToMessageDecoder {
  private int chunkSize;

  public RtmpDecoder() {
    chunkSize = Constants.DEFAULT_CHUNK_SIZE;
  }

  @Override
  protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf input, List<Object> output) throws Exception {
    if (input.readableBytes() < 7) {
      return;
    }

    int bzHd = input.getUnsignedByte(input.readerIndex());
    int fm = (bzHd >> 6) & 0x3;
    int chunkId = bzHd & 0x3F;
    if (chunkId > 64) {
      // TODO
    }

    int payLoadSize = 0;
    int headSize = 0;
    if (fm == 0) {
      headSize = 12;
      payLoadSize = input.getMedium(input.readerIndex() + 4);
    }
    if (fm == 1) {
      headSize = 8;
      payLoadSize = input.getMedium(input.readerIndex() + 4);
    }
    if (fm == 2 || fm == 3) {
      // something is missing
      ReferenceCountUtil.safeRelease(input);
      return;
    }

    int totalSize = headSize + payLoadSize + payLoadSize / chunkSize;

    if (input.readableBytes() < totalSize) {
      return;
    }

    RtmpMessage message = RtmpMessage.create();
    input.skipBytes(1);
    message.setFm(fm);
    message.setChunkStreamId(chunkId);
    message.setTimestamp(input.readMedium());
    input.skipBytes(3);
    message.setTypeId(input.readUnsignedByte());
    message.setStreamId(input.readIntLE());
    // payload
    if (payLoadSize > chunkSize) {
      int readSize = 0;
      while (readSize < payLoadSize) {
        int remain = payLoadSize - readSize;
        message.getPayload().writeBytes(input, Math.min(chunkSize, remain));
        readSize += Math.min(chunkSize, remain);
        if (remain > chunkSize) {
          input.skipBytes(1);
          readSize ++;
        }
      }
    } else {
      message.getPayload().writeBytes(input, payLoadSize);
    }
    output.add(message);
  }
}
