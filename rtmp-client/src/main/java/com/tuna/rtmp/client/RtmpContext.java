package com.tuna.rtmp.client;

public class RtmpContext {

  private String host;
  private int port;
  private String app;
  private String name;

  /**
   * while createStream, the rtmp server will give a stream id.
   */
  private int streamId;
  /**
   * connect param
   */
  private String flashver;
  private String swfUrl;
  private String tcUrl;
  private boolean fpad;
  private long audioCodecs;
  private long vidioCodecs;
  private long vidioFunction;
  private String pageUrl;
  private long capabilities;

  // about SPS, @see: 7.3.2.1.1, ISO_IEC_14496-10-AVC-2012.pdf, page 62
  private String h264Sps;
  private String h264Pps;
  // whether the sps and pps sent,
  // @see https://github.com/ossrs/srs/issues/203
  private boolean h264SpsPpsSent;
  // only send the ssp and pps when both changed.
  // @see https://github.com/ossrs/srs/issues/204
  private boolean h264SpsChanged;
  private boolean h264PpsChanged;
  // the aac sequence header.
  private String aacSpecificConfig;
  // user set timeout, in ms.
  private long stimeout;
  private long rtimeout;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getApp() {
    return app;
  }

  public void setApp(String app) {
    this.app = app;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFlashver() {
    return flashver;
  }

  public void setFlashver(String flashver) {
    this.flashver = flashver;
  }

  public String getSwfUrl() {
    return swfUrl;
  }

  public void setSwfUrl(String swfUrl) {
    this.swfUrl = swfUrl;
  }

  public String getTcUrl() {
    return tcUrl;
  }

  public void setTcUrl(String tcUrl) {
    this.tcUrl = tcUrl;
  }

  public boolean isFpad() {
    return fpad;
  }

  public void setFpad(boolean fpad) {
    this.fpad = fpad;
  }

  public long getAudioCodecs() {
    return audioCodecs;
  }

  public void setAudioCodecs(long audioCodecs) {
    this.audioCodecs = audioCodecs;
  }

  public long getVidioCodecs() {
    return vidioCodecs;
  }

  public void setVidioCodecs(long vidioCodecs) {
    this.vidioCodecs = vidioCodecs;
  }

  public long getVidioFunction() {
    return vidioFunction;
  }

  public void setVidioFunction(long vidioFunction) {
    this.vidioFunction = vidioFunction;
  }

  public String getPageUrl() {
    return pageUrl;
  }

  public void setPageUrl(String pageUrl) {
    this.pageUrl = pageUrl;
  }

  public long getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(long capabilities) {
    this.capabilities = capabilities;
  }

  public String getH264Sps() {
    return h264Sps;
  }

  public void setH264Sps(String h264Sps) {
    this.h264Sps = h264Sps;
  }

  public String getH264Pps() {
    return h264Pps;
  }

  public void setH264Pps(String h264Pps) {
    this.h264Pps = h264Pps;
  }

  public boolean isH264SpsPpsSent() {
    return h264SpsPpsSent;
  }

  public void setH264SpsPpsSent(boolean h264SpsPpsSent) {
    this.h264SpsPpsSent = h264SpsPpsSent;
  }

  public boolean isH264SpsChanged() {
    return h264SpsChanged;
  }

  public void setH264SpsChanged(boolean h264SpsChanged) {
    this.h264SpsChanged = h264SpsChanged;
  }

  public boolean isH264PpsChanged() {
    return h264PpsChanged;
  }

  public void setH264PpsChanged(boolean h264PpsChanged) {
    this.h264PpsChanged = h264PpsChanged;
  }

  public String getAacSpecificConfig() {
    return aacSpecificConfig;
  }

  public void setAacSpecificConfig(String aacSpecificConfig) {
    this.aacSpecificConfig = aacSpecificConfig;
  }

  public long getStimeout() {
    return stimeout;
  }

  public void setStimeout(long stimeout) {
    this.stimeout = stimeout;
  }

  public long getRtimeout() {
    return rtimeout;
  }

  public void setRtimeout(long rtimeout) {
    this.rtimeout = rtimeout;
  }

  public int getStreamId() {
    return streamId;
  }

  public void setStreamId(int streamId) {
    this.streamId = streamId;
  }
}
