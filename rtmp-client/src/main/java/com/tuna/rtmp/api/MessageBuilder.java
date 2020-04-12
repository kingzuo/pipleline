package com.tuna.rtmp.api;

import com.tuna.rtmp.client.RtmpContext;

public class MessageBuilder implements Constants {

  public RtmpMessage createConnect(long transId, RtmpContext context) {
    RtmpMessage message = RtmpMessage.create();
    message.setFm(0);
    message.setChunkStreamId(3);
    message.setTimestamp(0);
    message.setTypeId(MSG_TYPE_AMF_CMD);
    message.setStreamId(0);
    ProtocolUtils.writeAmfString(message.getPayload(), AMF_CMD_CONNECT);
    ProtocolUtils.writeAmfNumber(message.getPayload(), transId);
    ProtocolUtils.writeAmfObjectStart(message.getPayload());
    ProtocolUtils.writeString(message.getPayload(), "app");
    ProtocolUtils.writeAmfString(message.getPayload(), context.getApp());
    ProtocolUtils.writeString(message.getPayload(), "flashVer");
    ProtocolUtils.writeAmfString(message.getPayload(), context.getFlashver());
    ProtocolUtils.writeString(message.getPayload(), "swfUrl");
    ProtocolUtils.writeAmfString(message.getPayload(), context.getSwfUrl());
    ProtocolUtils.writeString(message.getPayload(), "tcUrl");
    ProtocolUtils.writeAmfString(message.getPayload(), context.getTcUrl());
    ProtocolUtils.writeString(message.getPayload(), "fpad");
    ProtocolUtils.writeAmfBoolean(message.getPayload(), context.isFpad());
    ProtocolUtils.writeString(message.getPayload(), "capabilities");
    ProtocolUtils.writeAmfNumber(message.getPayload(), context.getCapabilities());
    ProtocolUtils.writeString(message.getPayload(), "audioCodecs");
    ProtocolUtils.writeAmfNumber(message.getPayload(), context.getAudioCodecs());
    ProtocolUtils.writeString(message.getPayload(), "videoCodecs");
    ProtocolUtils.writeAmfNumber(message.getPayload(), context.getVidioCodecs());
    ProtocolUtils.writeString(message.getPayload(), "videoFunction");
    ProtocolUtils.writeAmfNumber(message.getPayload(), context.getVidioFunction());
    ProtocolUtils.writeString(message.getPayload(), "pageUrl");
    ProtocolUtils.writeAmfString(message.getPayload(), context.getPageUrl());
    ProtocolUtils.writeString(message.getPayload(), "objectEncoding");
    ProtocolUtils.writeAmfNumber(message.getPayload(), 0);
    ProtocolUtils.writeAmfObjectEnd(message.getPayload());
    return message;
  }

  public RtmpMessage createAcknowledgement(int size) {
    RtmpMessage message = RtmpMessage.create();
    message.setFm(0);
    message.setChunkStreamId(3);
    message.setTimestamp(0);
    message.setTypeId(MSG_TYPE_ACK);
    message.setStreamId(0);
    message.getPayload().writeInt(size);
    return message;
  }

  public RtmpMessage createAcknowledgementWindowSize(int size) {
    RtmpMessage message = RtmpMessage.create();
    message.setFm(0);
    message.setChunkStreamId(2);
    message.setTimestamp(0);
    message.setTypeId(MSG_TYPE_ACK_SIZE);
    message.setStreamId(0);
    message.getPayload().writeInt(size);
    return message;
  }

  public RtmpMessage createSetPeerBandWidth(int size) {
    RtmpMessage message = RtmpMessage.create();
    message.setFm(0);
    message.setChunkStreamId(2);
    message.setTimestamp(0);
    message.setTypeId(MSG_TYPE_BANDWIDTH);
    message.setStreamId(0);
    message.getPayload().writeInt(size);
    message.getPayload().writeByte(2);
    return message;
  }

  public RtmpMessage createSetChunkSize(int size) {
    RtmpMessage message = RtmpMessage.create();
    message.setFm(0);
    message.setChunkStreamId(2);
    message.setTimestamp(0);
    message.setTypeId(MSG_TYPE_CHUNK_SIZE);
    message.setStreamId(0);
    message.getPayload().writeInt(size);
    return message;
  }

  public RtmpMessage createStream(long transId) {
    RtmpMessage message = RtmpMessage.create();
    message.setFm(0);
    message.setChunkStreamId(3);
    message.setTimestamp(0);
    message.setTypeId(MSG_TYPE_AMF_CMD);
    message.setStreamId(0);
    ProtocolUtils.writeAmfString(message.getPayload(), AMF_CMD_CREATE_STREAM);
    ProtocolUtils.writeAmfNumber(message.getPayload(), transId);
    ProtocolUtils.writeAmfNull(message.getPayload());
    return message;
  }

  public RtmpMessage createFCPublish(long transId, RtmpContext context) {
    RtmpMessage message = RtmpMessage.create();
    message.setFm(0);
    message.setChunkStreamId(3);
    message.setTimestamp(0);
    message.setTypeId(MSG_TYPE_AMF_CMD);
    message.setStreamId(0);
    ProtocolUtils.writeAmfString(message.getPayload(), AMF_CMD_FC_PUBLISH);
    ProtocolUtils.writeAmfNumber(message.getPayload(), transId);
    ProtocolUtils.writeAmfNull(message.getPayload());
    ProtocolUtils.writeAmfString(message.getPayload(), context.getName());
    return message;
  }

  public RtmpMessage createPublish(long transId, RtmpContext context) {
    RtmpMessage message = RtmpMessage.create();
    message.setFm(0);
    message.setChunkStreamId(4);
    message.setTimestamp(0);
    message.setTypeId(MSG_TYPE_AMF_CMD);
    message.setStreamId(context.getStreamId());
    ProtocolUtils.writeAmfString(message.getPayload(), AMF_CMD_PUBLISH);
    ProtocolUtils.writeAmfNumber(message.getPayload(), transId);
    ProtocolUtils.writeAmfNull(message.getPayload());
    ProtocolUtils.writeAmfString(message.getPayload(), context.getName());
    ProtocolUtils.writeAmfString(message.getPayload(), context.getApp());
    return message;
  }
}
