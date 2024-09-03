package com.applovin.ramtool;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;

import com.applovin.ramtool.cpu.CpuActivity;
import com.applovin.ramtool.databinding.ActivityMainBinding;
import com.applovin.ramtool.ram.RamActivity;

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
