package com.park.hardware;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;

import com.park.hardware.cpu.CpuActivity;
import com.park.hardware.databinding.ActivityMainBinding;
import com.park.hardware.ram.RamActivity;

public class MainActivity extends Activity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this));

        binding.cpuButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CpuActivity.class));
        });
        binding.ramButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RamActivity.class));
        });

        setContentView(binding.getRoot());
    }


}
