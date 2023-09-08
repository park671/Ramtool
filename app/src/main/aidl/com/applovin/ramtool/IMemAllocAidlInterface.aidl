// IMemAllocAidlInterface.aidl
package com.applovin.ramtool;

// Declare any non-default types here with import statements

interface IMemAllocAidlInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    boolean allocHeapMemory(int size);
    boolean allocJavaMemory(int size);
    void release();
    String testBandWidth();
    String testLatency();
}