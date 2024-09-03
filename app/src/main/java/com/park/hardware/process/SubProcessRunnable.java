package com.park.hardware.process;

public interface SubProcessRunnable {

    void run();

    void onError(String errorMessage);

}
