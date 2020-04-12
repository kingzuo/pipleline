package com.tuna.rtmp.codec;

import com.tuna.rtmp.api.Constants;
import com.tuna.rtmp.api.RtmpMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

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
 */
public class RtmpEncoder extends MessageToByteEncoder {

  private int chunkSize;

  public RtmpEncoder() {
    chunkSize = Constants.DEFAULT_CHUNK_SIZE;
  }

  @Override
  protected void encode(ChannelHandlerContext channelHandlerContext, Object input, ByteBuf out) throws Exception {
    if (input instanceof RtmpMessage) {
      RtmpMessage message = (RtmpMessage) input;
      try {
        if (message.getChunkStreamId() > 64) {
          // TODO
        }
        out.writeByte((message.getFm() << 6) | message.getChunkStreamId());
        if (message.getFm() != 3) {
          out.writeMedium(message.getTimestamp());
        }
        if (message.getFm() < 2) {
          out.writeMedium(message.getPayload().readableBytes());
          out.writeByte(message.getTypeId());
        }
        if (message.getFm() == 0) {
          out.writeIntLE(message.getStreamId());
        }

        // if payload size is greater than chunk size, need split the package.
        while (message.getPayload().readableBytes() > chunkSize) {
          out.writeBytes(message.getPayload(), message.getPayload().readerIndex(), chunkSize);
          message.getPayload().skipBytes(chunkSize);
          out.writeByte((3 << 6) | message.getChunkStreamId());
        }
        out.writeBytes(message.getPayload(), message.getPayload().readerIndex(), message.getPayload().readableBytes());
        message.getPayload().skipBytes(message.getPayload().readableBytes());
      } finally {
        message.recycle();
      }
    } else {
      throw new RuntimeException("Write a unknow message type");
    }
  }
}
