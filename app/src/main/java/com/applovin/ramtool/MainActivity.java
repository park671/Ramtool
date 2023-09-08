package com.applovin.ramtool;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.applovin.ramtool.ui.LineChartView;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private volatile int singleProcessMemAllocd = 0;
    private volatile int multiProcessMemAllocd = 0;

    private TextView singleProcessHeap, multiProcessHeap, singleProcessJava;
    private TextView ramAvail, ramTotal, ramStatus;
    private TextView copyLegacyBandWidth, copySimdBandWidth, fillBandWidth, daxpyBandWidth, dotBandWidth;
    private TextView copyLegacyMaxTime, copySimdMaxTime, fillMaxTime, daxpyMaxTime, dotMaxTime;

    private LineChartView latencyLineChartView;
    private View loadingMask;
    private TextView l1CacheLatencyTextView, memoryLatencyTextView;

    private Handler workerHandler;

    private LinearLayout container;
    private Button allButton, bandWidthButton, volumeButton, latencyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        HandlerThread handlerThread = new HandlerThread("sub_thread");
        handlerThread.start();
        workerHandler = new Handler(handlerThread.getLooper());
        setContentView(R.layout.activity_main);
        container = findViewById(R.id.test_item_container);

        initBandWidthUI();
        initLatencyUI();
        initVolumeUI();
        initSystemUI();

        volumeButton = findViewById(R.id.alloc_mem_button);
        volumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                workerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onBenchmarkStart();
                        startAllocMemory();
                    }
                });
            }
        });
        allButton = findViewById(R.id.all_button);
        allButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                workerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onBenchmarkStart();
                        startAllBenchmark();
                    }
                });
            }
        });
        bandWidthButton = findViewById(R.id.bandwidth_button);
        bandWidthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                workerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onBenchmarkStart();
                        startBandWidth();
                    }
                });
            }
        });
        latencyButton = findViewById(R.id.latency_button);
        latencyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                workerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onBenchmarkStart();
                        startLatency();
                    }
                });
            }
        });

        ensureSubProcessService(null);
        refreshSystemInfo();
    }

    private long totalMemory = 0;

    private void refreshSystemInfo() {
        new Thread(() -> {
            totalMemory = getTotalMemoryMB(MainActivity.this);
            ramTotal.setText(totalMemory + "MB");
            while (true) {
                ramAvail.post(() -> {
                    ramAvail.setText(getAvailMemoryMB(MainActivity.this) + "MB");
                    boolean lowMem = isLowMemory(MainActivity.this);
                    ramStatus.setText("" + lowMem);
                    ramStatus.setTextColor(lowMem ? Color.RED : Color.WHITE);
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void onBenchmarkStart() {
        bandWidthButton.post(() -> {
            singleProcessHeap.setTextColor(Color.GREEN);
            multiProcessHeap.setTextColor(Color.GREEN);
            singleProcessJava.setTextColor(Color.GREEN);
            bandWidthButton.setEnabled(false);
            volumeButton.setEnabled(false);
            allButton.setEnabled(false);
            Toast.makeText(MainActivity.this, "benchmark start", Toast.LENGTH_SHORT).show();
        });
    }

    private void onBenchmarkFinish() {
        bandWidthButton.post(() -> {
            bandWidthButton.setEnabled(true);
            volumeButton.setEnabled(true);
            allButton.setEnabled(true);
            Toast.makeText(MainActivity.this, "benchmark finish", Toast.LENGTH_SHORT).show();
        });
        System.gc();
    }

    private void initBandWidthUI() {

        TextView[] copyLegacy = initBandWidthTestItem("copy(legacy)");
        copyLegacyBandWidth = copyLegacy[0];
        copyLegacyMaxTime = copyLegacy[1];
        TextView[] copySimd = initBandWidthTestItem("copy(arm-simd)");
        copySimdBandWidth = copySimd[0];
        copySimdMaxTime = copySimd[1];
        TextView[] fill = initBandWidthTestItem("fill");
        fillBandWidth = fill[0];
        fillMaxTime = fill[1];
        TextView[] daxpy = initBandWidthTestItem("daxpy");
        daxpyBandWidth = daxpy[0];
        daxpyMaxTime = daxpy[1];
        TextView[] dot = initBandWidthTestItem("dot");
        dotBandWidth = dot[0];
        dotMaxTime = dot[1];
    }

    private void initTitle(String name) {

    }

    private void initLatencyUI() {
        View latencyView = View.inflate(this, R.layout.latency_view, null);
        l1CacheLatencyTextView = latencyView.findViewById(R.id.l1_cache_latency_tv);
        memoryLatencyTextView = latencyView.findViewById(R.id.memory_latency_tv);
        latencyLineChartView = latencyView.findViewById(R.id.latency_chartview);
        loadingMask = latencyView.findViewById(R.id.loading_mask);
        container.addView(latencyView);
    }

    private void setBandWidthUI(String msg) {
        copyLegacyBandWidth.post(() -> {
            copyLegacyBandWidth.setText(msg);
            copySimdBandWidth.setText(msg);
            fillBandWidth.setText(msg);
            daxpyBandWidth.setText(msg);
            dotBandWidth.setText(msg);
        });
    }

    private void initVolumeUI() {
        singleProcessHeap = initVolumeTestItem("single process heap");
        multiProcessHeap = initVolumeTestItem("multi process heap");
        singleProcessJava = initVolumeTestItem("java heap");
    }

    private void initSystemUI() {
        ramAvail = initSystemTestItem("avail memory (system)");
        ramTotal = initSystemTestItem("total memory (system)");
        ramStatus = initSystemTestItem("low memory (system)");
    }

    private TextView initVolumeTestItem(String name) {
        LinearLayout root = (LinearLayout) View.inflate(this, R.layout.storage_item, null);
        TextView nameTv = root.findViewById(R.id.name_tv);
        TextView scoreTv = root.findViewById(R.id.score_tv);
        nameTv.setText(name);
        container.addView(root);
        return scoreTv;
    }

    private TextView[] initBandWidthTestItem(String name) {
        LinearLayout root = (LinearLayout) View.inflate(this, R.layout.band_width_item, null);
        TextView nameTv = root.findViewById(R.id.name_tv);
        TextView scoreTv = root.findViewById(R.id.score_tv);
        TextView latencyTv = root.findViewById(R.id.lowest_tv);
        TextView[] result = new TextView[2];
        result[0] = scoreTv;
        result[1] = latencyTv;
        nameTv.setText(name);
        container.addView(root);
        return result;
    }

    private TextView initSystemTestItem(String name) {
        LinearLayout root = (LinearLayout) View.inflate(this, R.layout.system_item, null);
        TextView nameTv = root.findViewById(R.id.name_tv);
        TextView scoreTv = root.findViewById(R.id.score_tv);
        nameTv.setText(name);
        container.addView(root);
        return scoreTv;
    }

    private void ensureSubProcessService(Runnable runnable) {
        if (memAllocAidlInterface1 == null || memAllocAidlInterface2 == null || memAllocAidlInterface3 == null) {
            isSubProcessReady = false;
            setOnSubProcessReadyListener(runnable);
            try {
                unbindService(serviceConnection1);
            } catch (Throwable tr) {
            }
            try {
                unbindService(serviceConnection2);
            } catch (Throwable tr) {
            }
            try {
                unbindService(serviceConnection3);
            } catch (Throwable tr) {
            }
            bindService(new Intent(MainActivity.this, Sub1ProcessService.class), serviceConnection1, BIND_AUTO_CREATE);
            bindService(new Intent(MainActivity.this, Sub2ProcessService.class), serviceConnection2, BIND_AUTO_CREATE);
            bindService(new Intent(MainActivity.this, Sub3ProcessService.class), serviceConnection3, BIND_AUTO_CREATE);
        } else {
            onSubProcessReady();
            setOnSubProcessReadyListener(runnable);
        }
    }

    private IMemAllocAidlInterface memAllocAidlInterface1, memAllocAidlInterface2, memAllocAidlInterface3;

    private ServiceConnection serviceConnection1 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            memAllocAidlInterface1 = IMemAllocAidlInterface.Stub.asInterface(service);
            onSubProcessReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            memAllocAidlInterface1 = null;
        }
    };

    private ServiceConnection serviceConnection2 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            memAllocAidlInterface2 = IMemAllocAidlInterface.Stub.asInterface(service);
            onSubProcessReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            memAllocAidlInterface2 = null;
        }
    };

    private ServiceConnection serviceConnection3 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            memAllocAidlInterface3 = IMemAllocAidlInterface.Stub.asInterface(service);
            onSubProcessReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            memAllocAidlInterface3 = null;
        }
    };

    private volatile Runnable onSubProcessReadyListener = null;
    private volatile boolean isSubProcessReady = false;

    private synchronized void setOnSubProcessReadyListener(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (isSubProcessReady) {
            runnable.run();
        } else {
            onSubProcessReadyListener = runnable;
        }
    }

    private synchronized void onSubProcessReady() {
        Log.d(TAG, "onSubProcessReady");
        if (memAllocAidlInterface1 == null || memAllocAidlInterface2 == null || memAllocAidlInterface3 == null) {
            return;
        }
        Log.d(TAG, "real ready!");
        isSubProcessReady = true;
        if (onSubProcessReadyListener != null) {
            workerHandler.post(onSubProcessReadyListener);
            onSubProcessReadyListener = null;
        }
    }

    private void startAllBenchmark() {
        Log.d(TAG, "startAllocMemory was called!");
        setBandWidthUI("stream...");
        ensureSubProcessService(() -> {
            triggerBandWidth();
            ensureSubProcessService(() -> {
                triggerLatency();
                ensureSubProcessService(() -> {
                    singleProcessHeapMem();
                    ensureSubProcessService(() -> {
                        multiProcessMemAlloc();
                        ensureSubProcessService(() -> {
                            singleProcessJavaMemory();
                            onBenchmarkFinish();
                        });
                    });
                });
            });
        });
    }

    private void startAllocMemory() {
        Log.d(TAG, "startAllocMemory was called!");
        ensureSubProcessService(() -> {
            singleProcessHeapMem();
            ensureSubProcessService(() -> {
                multiProcessMemAlloc();
                ensureSubProcessService(() -> {
                    singleProcessJavaMemory();
                    onBenchmarkFinish();
                });

            });
        });
    }

    private void startBandWidth() {
        Log.d(TAG, "startAllocMemory was called!");
        setBandWidthUI("...");
        ensureSubProcessService(() -> {
            triggerBandWidth();
            onBenchmarkFinish();
        });
    }

    private void startLatency() {
        Log.d(TAG, "startLatency was called!");
        ensureSubProcessService(() -> {
            triggerLatency();
            onBenchmarkFinish();
        });
    }

    private void triggerLatency() {
        try {
            l1CacheLatencyTextView.post(() -> {
                l1CacheLatencyTextView.setText("...");
                memoryLatencyTextView.setText("...");
                loadingMask.setVisibility(View.VISIBLE);
                loadingMask.setClickable(true);
            });
            String result = memAllocAidlInterface1.testLatency();
//            String result = "0.00195:3.862#0.00293:4.064#0.00391:4.423#0.00586:4.406#0.00781:4.421#0.00977:4.421#0.01172:4.422#0.01367:4.418#0.01562:4.421#0.01758:4.422#0.01953:4.405#0.02148:4.421#0.02344:4.422#0.02539:4.422#0.02734:4.421#0.02930:4.422#0.03125:4.422#0.03516:4.422#0.03906:4.422#0.04297:4.406#0.04688:4.422#0.05078:4.422#0.05469:4.421#0.05859:4.421#0.06250:4.406#0.07031:6.376#0.07812:7.890#0.08594:7.976#0.09375:7.929#0.10156:7.894#0.10938:7.933#0.11719:7.890#0.12500:9.070#0.14062:7.930#0.15625:7.927#0.17188:7.897#0.18750:10.627#0.20312:13.287#0.21875:11.811#0.23438:11.497#0.25000:11.047#0.28125:15.184#0.31250:14.852#0.34375:16.491#0.37500:18.020#0.40625:19.514#0.43750:20.130#0.46875:20.556#0.50000:20.495#0.56250:20.648#0.62500:21.161#0.68750:21.365#0.75000:21.164#0.81250:22.177#0.87500:21.709#0.93750:22.233#1.00000:27.760#1.12500:38.057#1.25000:48.864#1.37500:53.717#1.50000:63.421#1.62500:60.014#1.75000:65.536#1.87500:87.077#2.00000:92.777#2.25000:79.745#2.50000:84.180#2.75000:87.299#3.00000:112.156#3.25000:112.507#3.50000:112.946#3.75000:113.011#4.00000:112.534#4.50000:98.513#5.00000:112.639#5.50000:112.869#6.00000:112.870#6.50000:112.698#7.00000:105.339#7.50000:105.441#8.00000:106.287#9.00000:107.267#10.00000:107.801#11.00000:108.434#12.00000:111.317#13.00000:113.054#14.00000:109.570#15.00000:109.924#16.00000:112.918#18.00000:110.381#20.00000:113.006#22.00000:110.880#24.00000:112.951#26.00000:111.278#28.00000:111.809#30.00000:111.402#32.00000:112.016#36.00000:112.982#40.00000:112.220#44.00000:112.174#48.00000:112.841#52.00000:112.490#56.00000:112.854#60.00000:112.504#64.00000:113.018#72.00000:112.779#80.00000:112.478#88.00000:112.943#96.00000:113.250#104.00000:113.039#112.00000:113.006#120.00000:113.042#128.00000:113.116";
            DataProcessor.getInstance().process(result);
            l1CacheLatencyTextView.post(() -> {
                loadingMask.setVisibility(View.GONE);
                loadingMask.setClickable(false);
                latencyLineChartView.setShowNum(DataProcessor.getInstance().getAllLatencys().size());
                l1CacheLatencyTextView.setText(String.format("%,.2f", DataProcessor.getInstance().getL1CacheLatency()) + "ns");
                memoryLatencyTextView.setText(String.format("%,.2f", DataProcessor.getInstance().getMemoryLatency()) + "ns");
                latencyLineChartView.setDatas(DataProcessor.getInstance().getAllLatencys(), DataProcessor.getInstance().getSizes());
            });
        } catch (Throwable ignore) {
        }
    }

    private void triggerBandWidth() {
        try {
            String result = memAllocAidlInterface1.testBandWidth();
            copyLegacyBandWidth.post(() -> {
                String[] splitResult = result.split("#");
                copyLegacyBandWidth.setText(splitResult[0].split(",")[0] + "MB/s");
                copySimdBandWidth.setText(splitResult[1].split(",")[0] + "MB/s");
                fillBandWidth.setText(splitResult[2].split(",")[0] + "MB/s");
                daxpyBandWidth.setText(splitResult[3].split(",")[0] + "MB/s");
                dotBandWidth.setText(splitResult[4].split(",")[0] + "MB/s");

                copyLegacyMaxTime.setText(splitResult[0].split(",")[3] + "ms");
                copySimdMaxTime.setText(splitResult[1].split(",")[3] + "ms");
                fillMaxTime.setText(splitResult[2].split(",")[3] + "ms");
                daxpyMaxTime.setText(splitResult[3].split(",")[3] + "ms");
                dotMaxTime.setText(splitResult[4].split(",")[3] + "ms");
            });
            Log.d(TAG, "triggerBandWidth finish");
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private void multiProcessMemAlloc() {
        boolean running = true;
        multiProcessMemAllocd = 0;
        while (running) {
            boolean success = false;
            try {
                success = memAllocAidlInterface1.allocHeapMemory(1)
                        && memAllocAidlInterface2.allocHeapMemory(1)
                        && memAllocAidlInterface3.allocHeapMemory(1);
            } catch (Throwable tr) {
                try {
                    memAllocAidlInterface1.release();
                } catch (Throwable ignore) {
                }
                try {
                    memAllocAidlInterface2.release();
                } catch (Throwable ignore) {
                }
                try {
                    memAllocAidlInterface3.release();
                } catch (Throwable ignore) {
                }
                memAllocAidlInterface1 = null;
                memAllocAidlInterface2 = null;
                memAllocAidlInterface3 = null;
                tr.printStackTrace();
            }
            if (success) {
                multiProcessMemAllocd += 3;
                multiProcessHeap.post(new Runnable() {
                    @Override
                    public void run() {
                        multiProcessHeap.setText(multiProcessMemAllocd + "MB");
                    }
                });
            } else {
                running = false;
                multiProcessHeap.post(new Runnable() {
                    @Override
                    public void run() {
                        multiProcessHeap.setTextColor(Color.RED);
                        multiProcessHeap.invalidate();
                        multiProcessHeap.setText(multiProcessMemAllocd + "MB (" + Math.round(multiProcessMemAllocd * 100.0d / totalMemory * 1.0d) + "%)");
                        Toast.makeText(MainActivity.this, "multi process malloc " + multiProcessMemAllocd + "MB mem in heap success!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void singleProcessHeapMem() {
        Log.d(TAG, "singleThreadMemAlloc was called");
        boolean running = true;
        singleProcessMemAllocd = 0;
        while (running) {
            boolean success = false;
            try {
                success = memAllocAidlInterface1.allocHeapMemory(1);
            } catch (Throwable tr) {
                try {
                    memAllocAidlInterface1.release();
                } catch (Throwable ignore) {
                }
                memAllocAidlInterface1 = null;
                tr.printStackTrace();
            }
            if (success) {
                singleProcessMemAllocd += 1;
                singleProcessHeap.post(new Runnable() {
                    @Override
                    public void run() {
                        singleProcessHeap.setText(singleProcessMemAllocd + "MB");
                    }
                });
            } else {
                running = false;
                singleProcessHeap.post(new Runnable() {
                    @Override
                    public void run() {
                        singleProcessHeap.setTextColor(Color.RED);
                        singleProcessHeap.invalidate();
                        singleProcessHeap.setText(singleProcessMemAllocd + "MB (" + Math.round(singleProcessMemAllocd * 100.0d / totalMemory * 1.0d) + "%)");
                        Toast.makeText(MainActivity.this, "single process malloc " + singleProcessMemAllocd + "MB mem in heap success!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void singleProcessJavaMemory() {
        boolean running = true;
        singleProcessMemAllocd = 0;
        while (running) {
            boolean success = false;
            try {
                success = memAllocAidlInterface3.allocJavaMemory(1);
            } catch (Throwable tr) {
                try {
                    memAllocAidlInterface3.release();
                } catch (Throwable ignore) {
                }
                memAllocAidlInterface3 = null;
                tr.printStackTrace();
            }
            if (success) {
                singleProcessMemAllocd += 1;
                singleProcessJava.post(new Runnable() {
                    @Override
                    public void run() {
                        singleProcessJava.setText(singleProcessMemAllocd + "MB");
                    }
                });
            } else {
                running = false;
                singleProcessJava.post(new Runnable() {
                    @Override
                    public void run() {
                        singleProcessJava.setTextColor(Color.RED);
                        singleProcessJava.invalidate();
                        Toast.makeText(MainActivity.this, "java malloc " + singleProcessMemAllocd + "MB mem in heap success!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    public static long getAvailMemoryMB(Activity activity) {
        if (activity == null) {
            return 0L;
        } else {
            ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) {
                return 0L;
            } else {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);
                return memoryInfo.availMem / 1048576L;
            }
        }
    }

    public static long getTotalMemoryMB(Activity activity) {
        if (activity == null) {
            return 0L;
        }
        ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        if (activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.totalMem / 1048576L;
        }
        return 0L;
    }

    public static boolean isLowMemory(Activity activity) {
        if (activity == null) {
            return false;
        }
        ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        if (activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.lowMemory;
        }
        return false;
    }

}
