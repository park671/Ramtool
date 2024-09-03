// ISubProcessService.aidl
package com.applovin.ramtool;

// Declare any non-default types here with import statements

interface ISubProcessService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    boolean allocHeapMemory(int size);
    boolean allocJavaMemory(int size);
    void release();
    String testBandWidth();
    String testLatency();
    String getCurrentLatency();
    boolean isLatencyFinish();

   boolean supportSVE();
   boolean supportSVE2();
}