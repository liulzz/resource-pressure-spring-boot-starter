package io.github.liulzz.resourcepressure;

public record ResourcePressureStatus(
        boolean running,
        int concurrency,
        double targetCpuPercent,
        double targetMemoryPercent,
        double measuredCpuPercent,
        double usedHeapPercent,
        int cpuWorkers,
        int retainedMemoryChunks
) {
}
