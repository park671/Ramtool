package com.park.hardware.ram;

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

import com.applovin.ramtool.ISubProcessService;
import com.park.hardware.R;
import com.park.hardware.process.Sub1ProcessService;
import com.park.hardware.process.Sub2ProcessService;
import com.park.hardware.process.Sub3ProcessService;
import com.park.hardware.ram.ui.LineChartView;


public class RamActivity extends Activity {

    private static final String TAG = "RamActivity";

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
        setContentView(R.layout.activity_ram);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isSubProcessReady = false;
        try {
            unbindService(serviceConnection1);
        } catch (Throwable ignore) {
        }
        try {
            unbindService(serviceConnection2);
        } catch (Throwable ignore) {
        }
        try {
            unbindService(serviceConnection3);
        } catch (Throwable ignore) {
        }
    }

    private long totalMemory = 0;

    private void refreshSystemInfo() {
        new Thread(() -> {
            totalMemory = getTotalMemoryMB(RamActivity.this);
            ramTotal.setText(totalMemory + "MB");
            while (true) {
                ramAvail.post(() -> {
                    ramAvail.setText(getAvailMemoryMB(RamActivity.this) + "MB");
                    boolean lowMem = isLowMemory(RamActivity.this);
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
            Toast.makeText(RamActivity.this, "benchmark start", Toast.LENGTH_SHORT).show();
        });
    }

    private void onBenchmarkFinish() {
        bandWidthButton.post(() -> {
            bandWidthButton.setEnabled(true);
            volumeButton.setEnabled(true);
            allButton.setEnabled(true);
            Toast.makeText(RamActivity.this, "benchmark finish", Toast.LENGTH_SHORT).show();
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
        if (subProcessService1 == null || subProcessService2 == null || subProcessService3 == null) {
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
            bindService(new Intent(RamActivity.this, Sub1ProcessService.class), serviceConnection1, BIND_AUTO_CREATE);
            bindService(new Intent(RamActivity.this, Sub2ProcessService.class), serviceConnection2, BIND_AUTO_CREATE);
            bindService(new Intent(RamActivity.this, Sub3ProcessService.class), serviceConnection3, BIND_AUTO_CREATE);
        } else {
            onSubProcessReady();
            setOnSubProcessReadyListener(runnable);
        }
    }

    private ISubProcessService subProcessService1, subProcessService2, subProcessService3;

    private ServiceConnection serviceConnection1 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            subProcessService1 = ISubProcessService.Stub.asInterface(service);
            onSubProcessReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            subProcessService1 = null;
        }
    };

    private ServiceConnection serviceConnection2 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            subProcessService2 = ISubProcessService.Stub.asInterface(service);
            onSubProcessReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            subProcessService2 = null;
        }
    };

    private ServiceConnection serviceConnection3 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            subProcessService3 = ISubProcessService.Stub.asInterface(service);
            onSubProcessReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            subProcessService3 = null;
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
        if (subProcessService1 == null || subProcessService2 == null || subProcessService3 == null) {
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
            String result = subProcessService1.testLatency();
            HandlerThread handlerThread = new HandlerThread("temp_latency_update_thread");
            handlerThread.start();
            Handler handler = new Handler(handlerThread.getLooper());
            triggerLatencyUpdate(handler, handlerThread);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void triggerLatencyUpdate(Handler handler, HandlerThread handlerThread) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = subProcessService1.getCurrentLatency();
                    try {
                        DataProcessor.getInstance().process(result);
                        l1CacheLatencyTextView.post(() -> {
                            latencyLineChartView.setShowNum(DataProcessor.getInstance().getAllLatencys().size());
                            l1CacheLatencyTextView.setText(String.format("%,.2f", DataProcessor.getInstance().getL1CacheLatency()) + "ns");
                            memoryLatencyTextView.setText(String.format("%,.2f", DataProcessor.getInstance().getMemoryLatency()) + "ns");
                            latencyLineChartView.setDatas(DataProcessor.getInstance().getAllLatencys(), DataProcessor.getInstance().getSizes());
                        });
                    } catch (Throwable tr) {
                        tr.printStackTrace();
                        Log.d(TAG, "data process error");
                    }
                    if (!subProcessService1.isLatencyFinish()) {
                        triggerLatencyUpdate(handler, handlerThread);
                    } else {
                        loadingMask.post(new Runnable() {
                            @Override
                            public void run() {
                                loadingMask.setVisibility(View.GONE);
                                loadingMask.setClickable(false);
                                handlerThread.quitSafely();
                            }
                        });
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 1000);
    }

    private void triggerBandWidth() {
        try {
            String result = subProcessService1.testBandWidth();
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
                success = subProcessService1.allocHeapMemory(1)
                        && subProcessService2.allocHeapMemory(1)
                        && subProcessService3.allocHeapMemory(1);
            } catch (Throwable tr) {
                try {
                    subProcessService1.release();
                } catch (Throwable ignore) {
                }
                try {
                    subProcessService2.release();
                } catch (Throwable ignore) {
                }
                try {
                    subProcessService3.release();
                } catch (Throwable ignore) {
                }
                subProcessService1 = null;
                subProcessService2 = null;
                subProcessService3 = null;
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
                        Toast.makeText(RamActivity.this, "multi process malloc " + multiProcessMemAllocd + "MB mem in heap success!", Toast.LENGTH_SHORT).show();
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
                success = subProcessService1.allocHeapMemory(1);
            } catch (Throwable tr) {
                try {
                    subProcessService1.release();
                } catch (Throwable ignore) {
                }
                subProcessService1 = null;
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
                        Toast.makeText(RamActivity.this, "single process malloc " + singleProcessMemAllocd + "MB mem in heap success!", Toast.LENGTH_SHORT).show();
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
                success = subProcessService3.allocJavaMemory(1);
            } catch (Throwable tr) {
                try {
                    subProcessService3.release();
                } catch (Throwable ignore) {
                }
                subProcessService3 = null;
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
                        Toast.makeText(RamActivity.this, "java malloc " + singleProcessMemAllocd + "MB mem in heap success!", Toast.LENGTH_SHORT).show();
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
