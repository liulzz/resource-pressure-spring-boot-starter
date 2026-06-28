package io.github.liulzz.resourcepressure;

public record ResourcePressureTarget(
        int concurrency,
        double cpuPercent,
        double memoryPercent,
        double fluctuationPercent
) {
}
