package com.tuna.rtmp.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.Recycler;
import io.netty.util.ReferenceCountUtil;

public class RtmpMessage {
  private final Recycler.Handle<RtmpMessage> recyclerHandle;
  private int fm;
  private int chunkStreamId;
  private int timestamp;
  private int typeId;
  private int streamId;
  private ByteBuf payload;

  private static final Recycler<RtmpMessage> RECYCLER = new Recycler<RtmpMessage>() {
    @Override
    protected RtmpMessage newObject(Handle<RtmpMessage> handle) {
      return new RtmpMessage(handle);
    }
  };

  public RtmpMessage(Recycler.Handle<RtmpMessage> recyclerHandle) {
    this.recyclerHandle = recyclerHandle;
  }

  public static RtmpMessage create() {
    RtmpMessage response = RECYCLER.get();
    response.setPayload(ByteBufAllocator.DEFAULT.buffer());
    return response;
  }

  public void recycle() {
    fm = -1;
    chunkStreamId = -1;
    timestamp = -1;
    typeId = -1;
    streamId = -1;
    payload.clear();
    ReferenceCountUtil.safeRelease(payload);
    recyclerHandle.recycle(this);
  }

  public int getFm() {
    return fm;
  }

  public void setFm(int fm) {
    this.fm = fm;
  }

  public int getChunkStreamId() {
    return chunkStreamId;
  }

  public void setChunkStreamId(int chunkStreamId) {
    this.chunkStreamId = chunkStreamId;
  }

  public int getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(int timestamp) {
    this.timestamp = timestamp;
  }

  public int getTypeId() {
    return typeId;
  }

  public void setTypeId(int typeId) {
    this.typeId = typeId;
  }

  public int getStreamId() {
    return streamId;
  }

  public void setStreamId(int streamId) {
    this.streamId = streamId;
  }

  public ByteBuf getPayload() {
    return payload;
  }

  public void setPayload(ByteBuf payload) {
    this.payload = payload;
  }
}
