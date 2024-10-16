package com.park.hardware.cpu;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.CheckBox;

import androidx.annotation.Nullable;

import com.applovin.ramtool.ISubProcessService;
import com.park.hardware.NativeBridge;
import com.park.hardware.databinding.ActivityCpuBinding;
import com.park.hardware.process.Sub1ProcessService;
import com.park.hardware.process.SubProcessRunnable;

import java.util.concurrent.CountDownLatch;

public class CpuActivity extends Activity {

    private static final String TAG = "CpuActivity";

    private ActivityCpuBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCpuBinding.inflate(LayoutInflater.from(this));
        binding.core2coreLatButton.setOnClickListener(v -> startCore2CoreLatencyTestReadMode());
        binding.sveAddButton.setOnClickListener(v -> {
            new Thread(() -> {
                testAllInst();
            }).start();
        });

        binding.occupyCpuButton.setOnClickListener(v -> {
            int threadCount = Runtime.getRuntime().availableProcessors() * 2;
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    NativeBridge.occupyCpu(30);
                }, "busy_load").start();
            }
        });

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

    private void setCheckBoxStatus(boolean status) {
        runOnUiThread(() -> {
            binding.aarch32SupportCheckBox.setEnabled(status);
            binding.aarch64SupportCheckBox.setEnabled(status);
            binding.vfpSupportCheckBox.setEnabled(status);
            binding.neonSupportCheckBox.setEnabled(status);
            binding.simdSupportCheckBox.setEnabled(status);
            binding.sveSupportCheckBox.setEnabled(status);
            binding.sve2SupportCheckBox.setEnabled(status);
            //crypto ext
            binding.aesSupportCheckBox.setEnabled(status);
            binding.sha1SupportCheckBox.setEnabled(status);
            binding.sha2SupportCheckBox.setEnabled(status);
            binding.pmullSupportCheckBox.setEnabled(status);
            binding.crc32SupportCheckBox.setEnabled(status);
        });
    }

    private void testInstBlock(final InstTestProvider provider, final CheckBox checkBox) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ensureSubProcessService(new SubProcessRunnable() {
            @Override
            public void run() {
                try {
                    if (provider.support()) {
                        runOnUiThread(() -> {
                            checkBox.setChecked(true);
                        });
                    } else {
                        runOnUiThread(() -> {
                            checkBox.setChecked(false);
                        });
                    }
                } catch (Throwable ignore) {
                    runOnUiThread(() -> {
                        checkBox.setChecked(false);
                    });
                } finally {
                    try {
                        countDownLatch.countDown();
                    } catch (Throwable ignore) {
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    checkBox.setEnabled(true);
                });
                try {
                    countDownLatch.countDown();
                } catch (Throwable ignore) {
                }
            }
        });
        try {
            countDownLatch.await();
        } catch (Throwable ignore) {
        }
    }

    private void testAllInst() {
        setCheckBoxStatus(false);
        testInstBlock(() -> subProcessService.testSve(), binding.sveSupportCheckBox);
        testInstBlock(() -> subProcessService.testSve2(), binding.sve2SupportCheckBox);
        testInstBlock(() -> {
            String[] supportedAbis = Build.SUPPORTED_ABIS;
            for (String abi : supportedAbis) {
                if (abi.equalsIgnoreCase("armeabi-v7a")) {
                    return true;
                }
            }
            return false;
        }, binding.aarch32SupportCheckBox);
        testInstBlock(() -> subProcessService.testAarch64(), binding.aarch64SupportCheckBox);
        testInstBlock(() -> subProcessService.testVfp(), binding.vfpSupportCheckBox);
        testInstBlock(() -> subProcessService.testNeon(), binding.neonSupportCheckBox);
        testInstBlock(() -> subProcessService.testAsimd(), binding.simdSupportCheckBox);

        testInstBlock(() -> subProcessService.testAes(), binding.aesSupportCheckBox);
        testInstBlock(() -> subProcessService.testSha1(), binding.sha1SupportCheckBox);
        testInstBlock(() -> subProcessService.testSha2(), binding.sha2SupportCheckBox);
        testInstBlock(() -> subProcessService.testPmull(), binding.pmullSupportCheckBox);
        testInstBlock(() -> subProcessService.testCrc32(), binding.crc32SupportCheckBox);
        setCheckBoxStatus(true);
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
