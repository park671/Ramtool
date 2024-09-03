package com.applovin.ramtool.cpu;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;

import com.applovin.ramtool.NativeBridge;
import com.applovin.ramtool.databinding.ActivityCpuBinding;

public class CpuActivity extends Activity {

    private static final String TAG = "CpuActivity";

    private ActivityCpuBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCpuBinding.inflate(LayoutInflater.from(this));
        binding.core2coreLatButton.setOnClickListener(v -> startCore2CoreLatencyTestReadMode());
        setContentView(binding.getRoot());
    }


    private void startCore2CoreLatencyTestReadMode() {
        new Thread(() -> {
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
