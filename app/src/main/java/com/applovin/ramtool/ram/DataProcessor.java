package com.applovin.ramtool.ram;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataProcessor {

    private static final String TAG = "DataProcessor";

    private static final DataProcessor sInstance = new DataProcessor();

    public static DataProcessor getInstance() {
        return sInstance;
    }

    private DataProcessor() {
    }

    private double l1CacheLatency, memoryLatency;
    private double l1CacheSize;
    private List<Double> latencys, sortedDiffLatency;
    private List<String> sizes;

    class Platform implements Comparable {
        public Double latency;
        public int count;

        @Override
        public int compareTo(Object o) {
            if (o instanceof Platform) {
                return ((Platform) o).count - this.count;
            }
            return 0;
        }
    }

    public void process(String latencyResult) {
        Log.d(TAG, latencyResult);
        if (latencys == null) {
            latencys = new ArrayList<>();
            sizes = new ArrayList<>();
        }
        latencys.clear();
        sizes.clear();
        String[] items = latencyResult.split("#");
        for (int i = 0; i < items.length; i++) {
            String[] keyValue = items[i].split(":");
            String sizeStr = keyValue[0];
            String latencyStr = keyValue[1];
            latencys.add(Double.parseDouble(latencyStr));
            sizes.add(sizeStr);
        }
        try {
            AnalyzeData();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void AnalyzeData() {
        List<Double> sortedLatency = new ArrayList<>(latencys);
        Collections.sort(sortedLatency);
        sortedDiffLatency = new ArrayList<>();
        sortedDiffLatency.add(0.0);
        for (int i = 1; i < sortedLatency.size(); i++) {
            sortedDiffLatency.add(sortedLatency.get(i) - sortedLatency.get(i - 1));
        }
        List<Platform> platforms = new ArrayList<>();
        int start = -1;
        for (int i = 0; i < sortedDiffLatency.size(); i++) {
            if (sortedDiffLatency.get(i) < 1.0d && i < sortedDiffLatency.size() - 1) {
                if (start == -1) {
                    start = i;
                }
            } else {
                if (start != -1) {
                    Log.d(TAG, "from:" + start + " to " + i);
                    int count = i - start;
                    Platform platform = new Platform();
                    platform.count = count;
                    platform.latency = 0.d;
                    for (int j = start; j < i; j++) {
                        platform.latency += sortedLatency.get(j);
                    }
                    platform.latency /= count;
                    platforms.add(platform);
                    start = -1;
                }
            }
        }
        Collections.sort(platforms);
        for (int i = 0; i < 4; i++) {
            Platform platform = platforms.get(i);
            Log.d(TAG, "platform:" + i + ", latency:" + platform.latency + ", count = " + platform.count);
            if (i == 0) {
                l1CacheLatency = platform.latency;
                memoryLatency = platform.latency;
            } else {
                if (l1CacheLatency > platform.latency) {
                    l1CacheLatency = platform.latency;
                } else if (memoryLatency < platform.latency) {
                    memoryLatency = platform.latency;
                }
            }
        }
    }

    public double getL1CacheLatency() {
        return l1CacheLatency;
    }

    public double getMemoryLatency() {
        return memoryLatency;
    }

    public List<Double> getAllLatencys() {
        return latencys;
    }

    public List<Double> getSortedDiff() {
        return latencys;
    }

    public List<String> getSizes() {
        return sizes;
    }
}
