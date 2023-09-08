package com.applovin.ramtool;

public class NativeBridge {

    static {
        System.loadLibrary("ramtool");
    }

    public static native boolean allocHeapMemory(int sizeInMB);
    public static native void release();
    public static native String testBandWidth();

    public static native String testLatency();

}
