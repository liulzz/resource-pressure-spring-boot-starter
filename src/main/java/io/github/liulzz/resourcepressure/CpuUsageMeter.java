package io.github.liulzz.resourcepressure;

@FunctionalInterface
public interface CpuUsageMeter {
    double measureProcessCpuPercent();
}
