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
    
    boolean testAarch64();
    boolean testVfp();
    
    boolean testNeon();
    boolean testAsimd();
    boolean testSve();
    boolean testSve2();
    
    boolean testAes();
    boolean testSha1();
    boolean testSha2();
    boolean testPmull();
    boolean testCrc32();
}