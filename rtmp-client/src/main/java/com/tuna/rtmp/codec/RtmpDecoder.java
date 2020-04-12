package com.tuna.rtmp.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
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
 */
public class RtmpDecoder extends ByteToMessageDecoder {

  public RtmpDecoder() {
  }

  @Override
  protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf input, List<Object> output) throws Exception {
    
  }
}
