package com.park.hardware.cpu;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;

import com.applovin.ramtool.ISubProcessService;
import com.park.hardware.NativeBridge;
import com.park.hardware.databinding.ActivityCpuBinding;
import com.park.hardware.process.Sub1ProcessService;
import com.park.hardware.process.SubProcessRunnable;

public class CpuActivity extends Activity {

    private static final String TAG = "CpuActivity";

    private ActivityCpuBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCpuBinding.inflate(LayoutInflater.from(this));
        binding.core2coreLatButton.setOnClickListener(v -> startCore2CoreLatencyTestReadMode());
        binding.sveAddButton.setOnClickListener(v -> sveAddTest());
        setContentView(binding.getRoot());
    }

    private ISubProcessService subProcessService;

    private SubProcessRunnable onSubProcessReadyListener;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            subProcessService = ISubProcessService.Stub.asInterface(service);
            if (onSubProcessReadyListener != null) {
                onSubProcessReadyListener.run();
                onSubProcessReadyListener = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            subProcessService = null;
            if (onSubProcessReadyListener != null) {
                onSubProcessReadyListener.onError("remote died");
                onSubProcessReadyListener = null;
            }
        }
    };

    private void ensureSubProcessService(SubProcessRunnable runnable) {
        if (onSubProcessReadyListener != null) {
            Log.e(TAG, "already has task");
            return;
        }
        onSubProcessReadyListener = runnable;
        if (subProcessService != null) {
            try {
                unbindService(serviceConnection);
            } catch (Throwable ignore) {
            }
        }
        if (!bindService(new Intent(CpuActivity.this, Sub1ProcessService.class), serviceConnection, BIND_AUTO_CREATE)) {
            runnable.onError("connect fail");
        }
    }

    private void sveAddTest() {
        runOnUiThread(() -> {
            binding.sveSupportCheckBox.setEnabled(false);
            binding.sve2SupportCheckBox.setEnabled(false);
        });
        ensureSubProcessService(new SubProcessRunnable() {
            @Override
            public void run() {
                try {
                    if (subProcessService.supportSVE()) {
                        runOnUiThread(() -> {
                            binding.sveSupportCheckBox.setChecked(true);
                        });
                    } else {
                        runOnUiThread(() -> {
                            binding.sveSupportCheckBox.setChecked(false);
                        });
                    }
                } catch (Throwable ignore) {
                    runOnUiThread(() -> {
                        binding.sveSupportCheckBox.setChecked(false);
                    });
                }
                //sve2
                try {
                    if (subProcessService.supportSVE2()) {
                        runOnUiThread(() -> {
                            binding.sve2SupportCheckBox.setChecked(true);
                        });
                    } else {
                        runOnUiThread(() -> {
                            binding.sve2SupportCheckBox.setChecked(false);
                        });
                    }
                } catch (Throwable ignore) {
                    runOnUiThread(() -> {
                        binding.sve2SupportCheckBox.setChecked(false);
                    });
                }
                runOnUiThread(() -> {
                    binding.sveSupportCheckBox.setEnabled(true);
                    binding.sve2SupportCheckBox.setEnabled(true);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    binding.sveSupportCheckBox.setEnabled(true);
                    binding.sve2SupportCheckBox.setEnabled(true);
                });
            }
        });
    }

    private void startCore2CoreLatencyTestReadMode() {
        new Thread(() -> {
            runOnUiThread(() -> {
                binding.readLatView.startProgress();
            });
            NativeBridge.benchCpuCore2CoreLatency(Core2CoreLatMode.READ_MODE, new OnCore2CoreLatencyCallback() {
                @Override
                public void onResult(int[][] lat) {
                    for (int i = 0; i < lat.length; i++) {
                        for (int j = 0; j < lat.length; j++) {
                            Log.d(TAG, "lat(" + i + "," + j + ")=" + lat[i][j]);
                        }
                    }
                    runOnUiThread(() -> {
                        binding.readLatView.update(lat, Color.RED);
                    });
                    startCore2CoreLatencyTestWriteMode();
                }

                @Override
                public void onError(String message) {

                }
            });
        }).start();
    }

    private void startCore2CoreLatencyTestWriteMode() {
        new Thread(() -> {
            runOnUiThread(() -> {
                binding.writeLatView.startProgress();
            });
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            NativeBridge.benchCpuCore2CoreLatency(Core2CoreLatMode.WRITE_MODE, new OnCore2CoreLatencyCallback() {
                @Override
                public void onResult(int[][] lat) {
                    for (int i = 0; i < lat.length; i++) {
                        for (int j = 0; j < lat.length; j++) {
                            Log.d(TAG, "lat(" + i + "," + j + ")=" + lat[i][j]);
                        }
                    }
                    runOnUiThread(() -> {
                        binding.writeLatView.update(lat, Color.BLUE);
                    });
                }

                @Override
                public void onError(String message) {

                }
            });
        }).start();
    }

}
