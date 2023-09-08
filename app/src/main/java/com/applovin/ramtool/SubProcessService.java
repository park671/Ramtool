package com.applovin.ramtool;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.Random;

public class SubProcessService extends Service {

    private static final String TAG = "SubProcessService";

    @Override
    public IBinder onBind(Intent intent) {
        return new SubProcessServiceBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private boolean realAllocHeapMemory(int size) {
        return NativeBridge.allocHeapMemory(size);
    }

    private long[][] array = null;
    private int index = 0;
    private Random random = new Random();

    private boolean realAllocJavaMemory(int size) {
        if (array == null) {
            array = new long[1024][];
        }
        int count = ((1024 * 1024) / 8) * size;
        array[index] = new long[count];
        for (int i = 0; i < count; i++) {
            array[index][i] = random.nextInt();
        }
        index++;
        return true;
    }

    class SubProcessServiceBinder extends IMemAllocAidlInterface.Stub {

        @Override
        public boolean allocHeapMemory(int size) throws RemoteException {
            return realAllocHeapMemory(size);
        }

        @Override
        public boolean allocJavaMemory(int size) throws RemoteException {
            return realAllocJavaMemory(size);
        }

        @Override
        public void release() throws RemoteException {
            array = null;
            NativeBridge.release();
        }

        @Override
        public String testBandWidth() throws RemoteException {
            return NativeBridge.testBandWidth();
        }

        @Override
        public String testLatency() throws RemoteException {
            return NativeBridge.testLatency();
        }

    }
}
