package com.park.hardware;

import com.park.hardware.cpu.Core2CoreLatMode;
import com.park.hardware.cpu.OnCore2CoreLatencyCallback;

public class NativeBridge {

    static {
        System.loadLibrary("hardwaretool");
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


    public static native void testAarch64();
    public static native void testVfp();
    public static native void testNeon();
    public static native void testAsimd();
    public static native void testSve();
    public static native void testSve2();
    public static native void testAes();
    public static native void testSha1();
    public static native void testSha2();
    public static native void testPmull();
    public static native void testCrc32();

    public static native void occupyCpu(int occupy_duration);

}
