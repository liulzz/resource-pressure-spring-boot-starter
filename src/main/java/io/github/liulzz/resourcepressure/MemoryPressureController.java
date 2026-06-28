package io.github.liulzz.resourcepressure;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

final class MemoryPressureController {
    private final ResourcePressureProperties properties;
    private final Queue<byte[]> retainedChunks = new ConcurrentLinkedQueue<>();

    MemoryPressureController(ResourcePressureProperties properties) {
        this.properties = properties;
    }

    void adjust(ResourcePressureTarget target) {
        Runtime runtime = Runtime.getRuntime();
        long maxHeap = runtime.maxMemory();
        double targetRatio = clamp(target.memoryPercent() / 100d, 0d, properties.getMaxHeapAllocationRatio());
        long targetBytes = (long) (maxHeap * targetRatio);
        long toleranceBytes = (long) (maxHeap * target.fluctuationPercent() / 100d);
        long used = usedHeapBytes(runtime);
        int chunkBytes = Math.max(4096, properties.getMemoryChunkBytes());
        long hardCap = (long) (maxHeap * clamp(properties.getMaxHeapAllocationRatio(), 0d, 0.98d));

        while (used + chunkBytes < targetBytes - toleranceBytes && retainedBytes() + chunkBytes < hardCap) {
            retainedChunks.add(new byte[chunkBytes]);
            used += chunkBytes;
        }
        while (used > targetBytes + toleranceBytes && !retainedChunks.isEmpty()) {
            byte[] removed = retainedChunks.poll();
            if (removed == null) {
                break;
            }
            used -= removed.length;
        }
    }

    double usedHeapPercent() {
        Runtime runtime = Runtime.getRuntime();
        return (double) usedHeapBytes(runtime) / (double) runtime.maxMemory() * 100d;
    }

    int retainedChunkCount() {
        return retainedChunks.size();
    }

    void release() {
        retainedChunks.clear();
    }

    private long retainedBytes() {
        return (long) retainedChunks.size() * Math.max(4096, properties.getMemoryChunkBytes());
    }

    private static long usedHeapBytes(Runtime runtime) {
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
