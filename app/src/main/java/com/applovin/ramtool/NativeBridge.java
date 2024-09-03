package com.applovin.ramtool;

import com.applovin.ramtool.cpu.Core2CoreLatMode;
import com.applovin.ramtool.cpu.OnCore2CoreLatencyCallback;

public class NativeBridge {

    static {
        System.loadLibrary("ramtool");
    }

    public static native boolean allocHeapMemory(int sizeInMB);

    public static native void release();

    public static native String testBandWidth();

    public static native String testLatency();

    public static native String getCurrentLatency();

    private static native void testCore2CoreLatency(int mode);

    private static volatile OnCore2CoreLatencyCallback core2CoreLatencyCallback = null;

    public static synchronized void benchCpuCore2CoreLatency(@Core2CoreLatMode int mode, OnCore2CoreLatencyCallback callback) {
        if (core2CoreLatencyCallback != null) {
            callback.onError("bench already start");
            return;
        }
        core2CoreLatencyCallback = callback;
        new Thread(() -> {
            testCore2CoreLatency(mode);
        }).start();
    }

    @SuppressWarnings("all")
    private static void onCore2CoreLatencyFinish(int[][] result) {
        core2CoreLatencyCallback.onResult(result);
        core2CoreLatencyCallback = null;
    }

}
