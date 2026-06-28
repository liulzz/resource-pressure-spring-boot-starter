package io.github.liulzz.resourcepressure;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

final class ThreadMxBeanCpuUsageMeter implements CpuUsageMeter {
    private final ThreadMXBean threadMxBean;
    private volatile long lastWallNanos;
    private volatile long lastCpuNanos;

    ThreadMxBeanCpuUsageMeter() {
        this(ManagementFactory.getThreadMXBean());
    }

    ThreadMxBeanCpuUsageMeter(ThreadMXBean threadMxBean) {
        this.threadMxBean = threadMxBean;
        if (threadMxBean.isThreadCpuTimeSupported() && !threadMxBean.isThreadCpuTimeEnabled()) {
            threadMxBean.setThreadCpuTimeEnabled(true);
        }
        this.lastWallNanos = System.nanoTime();
        this.lastCpuNanos = currentProcessCpuNanos();
    }

    @Override
    public synchronized double measureProcessCpuPercent() {
        long nowWall = System.nanoTime();
        long nowCpu = currentProcessCpuNanos();
        long wallDelta = nowWall - lastWallNanos;
        long cpuDelta = nowCpu - lastCpuNanos;
        lastWallNanos = nowWall;
        lastCpuNanos = nowCpu;
        if (wallDelta <= 0L || cpuDelta < 0L) {
            return 0d;
        }
        return (double) cpuDelta / (double) wallDelta * 100d;
    }

    private long currentProcessCpuNanos() {
        return Arrays.stream(threadMxBean.getAllThreadIds())
                .map(threadMxBean::getThreadCpuTime)
                .filter(value -> value > 0L)
                .sum();
    }
}
