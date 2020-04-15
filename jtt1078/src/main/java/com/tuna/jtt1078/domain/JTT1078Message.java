package com.tuna.jtt1078.domain;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.Recycler;
import io.netty.util.ReferenceCountUtil;

public class JTT1078Message {

  private final Recycler.Handle<JTT1078Message> recyclerHandle;
  private int loadType;
  private int sequenceNo;
  private long simNo;
  private int logicalNo;
  private int frameType;
  private int packType;
  private long timestamp;
  private int dts;
  private int pts;
  private ByteBuf payload;

  private static final Recycler<JTT1078Message> RECYCLER = new Recycler<JTT1078Message>() {
    @Override
    protected JTT1078Message newObject(Handle<JTT1078Message> handle) {
      return new JTT1078Message(handle);
    }
  };

  public JTT1078Message(Recycler.Handle<JTT1078Message> recyclerHandle) {
    this.recyclerHandle = recyclerHandle;
  }

  public static JTT1078Message create() {
    JTT1078Message response = RECYCLER.get();
    response.setPayload(ByteBufAllocator.DEFAULT.buffer());
    return response;
  }

  public void recycle() {
    loadType = -1;
    sequenceNo = -1;
    simNo = -1;
    logicalNo = -1;
    frameType = -1;
    packType = -1;
    timestamp = -1;
    dts = -1;
    pts = -1;
    payload.clear();
    ReferenceCountUtil.safeRelease(payload);
    recyclerHandle.recycle(this);
  }

  public int getLoadType() {
    return loadType;
  }

  public void setLoadType(int loadType) {
    this.loadType = loadType;
  }

  public int getSequenceNo() {
    return sequenceNo;
  }

  public void setSequenceNo(int sequenceNo) {
    this.sequenceNo = sequenceNo;
  }

  public long getSimNo() {
    return simNo;
  }

  public void setSimNo(long simNo) {
    this.simNo = simNo;
  }

  public int getLogicalNo() {
    return logicalNo;
  }

  public void setLogicalNo(int logicalNo) {
    this.logicalNo = logicalNo;
  }

  public int getFrameType() {
    return frameType;
  }

  public void setFrameType(int frameType) {
    this.frameType = frameType;
  }

  public int getPackType() {
    return packType;
  }

  public void setPackType(int packType) {
    this.packType = packType;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public int getDts() {
    return dts;
  }

  public void setDts(int dts) {
    this.dts = dts;
  }

  public int getPts() {
    return pts;
  }

  public void setPts(int pts) {
    this.pts = pts;
  }

  public ByteBuf getPayload() {
    return payload;
  }

  protected void setPayload(ByteBuf payload) {
    this.payload = payload;
  }
}
