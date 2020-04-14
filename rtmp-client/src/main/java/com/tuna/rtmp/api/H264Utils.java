package com.tuna.rtmp.api;

public class H264Utils {

  public static boolean isPPS(int value) {
    return value == 8;
  }

  public static boolean isSPS(int value) {
    return value == 7;
  }

  public static boolean isKeyFrame(int value) {
    return value == 5;
  }

  public static boolean isIgnore(int value) {
    if (value == 5 || value == 7 || value == 8 || value == 9 || value == 1) {
      return false;
    }
    return true;
  }
}
