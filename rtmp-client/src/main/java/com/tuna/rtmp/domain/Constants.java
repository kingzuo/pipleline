package com.tuna.rtmp.domain;

public interface Constants {

  int DEFAULT_CHUNK_SIZE = 128;

  int VERSION = 3;

  int C1C2_LENGTH = 1536;

  int AMF_TYPE_NUMBER = 0x00;
  int AMF_TYPE_BOOLEAN = 0x01;
  int AMF_TYPE_STRING = 0x02;
  int AMF_TYPE_OBJECT = 0x03;
  int AMF_TYPE_NULL = 0x05;
  int AMF_TYPE_ARRAY_NULL = 0x06;
  int AMF_TYPE_MIXED_ARRAY = 0x08;
  int AMF_TYPE_END = 0x09;
  int AMF_TYPE_ARRAY = 0x0a;

  int MSG_TYPE_CHUNK_SIZE = 1;
  int MSG_TYPE_ABORT = 2;
  int MSG_TYPE_ACK = 3;
  int MSG_TYPE_USER = 4;
  int MSG_TYPE_ACK_SIZE = 5;
  int MSG_TYPE_BANDWIDTH = 6;
  int MSG_TYPE_EDGE = 7;
  int MSG_TYPE_AUDIO = 8;
  int MSG_TYPE_VIDEO = 9;
  int MSG_TYPE_AMF3_META = 15;
  int MSG_TYPE_AMF3_SHARED = 16;
  int MSG_TYPE_AMF3_CMD = 17;
  int MSG_TYPE_AMF_META = 18;
  int MSG_TYPE_AMF_SHARED = 19;
  int MSG_TYPE_AMF_CMD = 20;
  int MSG_TYPE_AGGREGATE = 22;
  int MSG_TYPE_MAX = 22;
  int MSG_TYPE_UNDEFINED = 255;

  String AMF_CMD_CONNECT = "connect";
  String AMF_CMD_CREATE_STREAM = "createStream";
  String AMF_CMD_FC_PUBLISH = "FCPublish";
  String AMF_CMD_PUBLISH = "publish";
}
