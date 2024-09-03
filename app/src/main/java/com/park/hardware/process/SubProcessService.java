package com.park.hardware.process;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.applovin.ramtool.ISubProcessService;
import com.park.hardware.NativeBridge;

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

    private volatile boolean isLatencyFinished = false;
    private volatile String latencyStringResult;

    class SubProcessServiceBinder extends ISubProcessService.Stub {

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
            isLatencyFinished = false;
            new Thread(() -> {
                latencyStringResult = NativeBridge.testLatency();
                isLatencyFinished = true;
            }, "latency_thread").start();
            return "0.00195:3.862#0.00293:4.064#0.00391:4.423#0.00586:4.406#0.00781:4.421#0.00977:4.421#0.01172:4.422#0.01367:4.418#0.01562:4.421#0.01758:4.422#0.01953:4.405#0.02148:4.421#0.02344:4.422#0.02539:4.422#0.02734:4.421#0.02930:4.422#0.03125:4.422#0.03516:4.422#0.03906:4.422#0.04297:4.406#0.04688:4.422#0.05078:4.422#0.05469:4.421#0.05859:4.421#0.06250:4.406#0.07031:6.376#0.07812:7.890#0.08594:7.976#0.09375:7.929#0.10156:7.894#0.10938:7.933#0.11719:7.890#0.12500:9.070#0.14062:7.930#0.15625:7.927#0.17188:7.897#0.18750:10.627#0.20312:13.287#0.21875:11.811#0.23438:11.497#0.25000:11.047#0.28125:15.184#0.31250:14.852#0.34375:16.491#0.37500:18.020#0.40625:19.514#0.43750:20.130#0.46875:20.556#0.50000:20.495#0.56250:20.648#0.62500:21.161#0.68750:21.365#0.75000:21.164#0.81250:22.177#0.87500:21.709#0.93750:22.233#1.00000:27.760#1.12500:38.057#1.25000:48.864#1.37500:53.717#1.50000:63.421#1.62500:60.014#1.75000:65.536#1.87500:87.077#2.00000:92.777#2.25000:79.745#2.50000:84.180#2.75000:87.299#3.00000:112.156#3.25000:112.507#3.50000:112.946#3.75000:113.011#4.00000:112.534#4.50000:98.513#5.00000:112.639#5.50000:112.869#6.00000:112.870#6.50000:112.698#7.00000:105.339#7.50000:105.441#8.00000:106.287#9.00000:107.267#10.00000:107.801#11.00000:108.434#12.00000:111.317#13.00000:113.054#14.00000:109.570#15.00000:109.924#16.00000:112.918#18.00000:110.381#20.00000:113.006#22.00000:110.880#24.00000:112.951#26.00000:111.278#28.00000:111.809#30.00000:111.402#32.00000:112.016#36.00000:112.982#40.00000:112.220#44.00000:112.174#48.00000:112.841#52.00000:112.490#56.00000:112.854#60.00000:112.504#64.00000:113.018#72.00000:112.779#80.00000:112.478#88.00000:112.943#96.00000:113.250#104.00000:113.039#112.00000:113.006#120.00000:113.042#128.00000:113.116";
        }

        @Override
        public String getCurrentLatency() throws RemoteException {
            if (isLatencyFinished) {
                return latencyStringResult;
            }
            return NativeBridge.getCurrentLatency();
        }

        @Override
        public boolean isLatencyFinish() throws RemoteException {
            return isLatencyFinished;
        }

        @Override
        public boolean supportSVE() throws RemoteException {
            try {
                NativeBridge.testSve();
                return true;
            } catch (Throwable ignore) {
            }
            return false;
        }

        @Override
        public boolean supportSVE2() throws RemoteException {
            try {
                NativeBridge.testSve2();
                return true;
            } catch (Throwable ignore) {
            }
            return false;
        }

    }
}
