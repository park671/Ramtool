package com.applovin.ramtool.cpu;

public interface OnCore2CoreLatencyCallback {

    void onResult(int[][] lat);

    void onError(String message);

}
