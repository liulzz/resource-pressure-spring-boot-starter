package io.github.liulzz.resourcepressure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class CpuPressureController {
    private final ResourcePressureProperties properties;
    private final CpuUsageMeter cpuUsageMeter;
    private final ExecutorService executorService;
    private final List<Future<?>> workers = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile double dutyCycle;
    private volatile double measuredCpuPercent;
    private volatile double targetCpuPercent;

    CpuPressureController(ResourcePressureProperties properties, CpuUsageMeter cpuUsageMeter) {
        this.properties = properties;
        this.cpuUsageMeter = cpuUsageMeter;
        this.executorService = Executors.newCachedThreadPool(new NamedDaemonThreadFactory());
    }

    synchronized void start(ResourcePressureTarget target) {
        stopWorkers();
        this.targetCpuPercent = Math.max(0d, target.cpuPercent());
        int availableProcessors = Math.max(1, Runtime.getRuntime().availableProcessors());
        int workerCount = Math.max(1, Math.min(availableProcessors, (int) Math.ceil(targetCpuPercent / 100d)));
        this.dutyCycle = clamp(targetCpuPercent / (workerCount * 100d), 0d, 0.98d);
        running.set(true);
        for (int i = 0; i < workerCount; i++) {
            workers.add(executorService.submit(this::runWorker));
        }
    }

    synchronized void feedback(ResourcePressureTarget target) {
        if (!running.get()) {
            start(target);
            return;
        }
        this.targetCpuPercent = Math.max(0d, target.cpuPercent());
        this.measuredCpuPercent = Math.max(0d, cpuUsageMeter.measureProcessCpuPercent());
        double tolerance = targetCpuPercent * target.fluctuationPercent() / 100d;
        if (Math.abs(targetCpuPercent - measuredCpuPercent) <= tolerance) {
            return;
        }
        int divisor = Math.max(1, workers.size()) * 100;
        double error = (targetCpuPercent - measuredCpuPercent) / divisor;
        this.dutyCycle = clamp(dutyCycle + error * 0.5d, 0d, 0.98d);
    }

    double measuredCpuPercent() {
        return measuredCpuPercent;
    }

    synchronized int workerCount() {
        return workers.size();
    }

    synchronized void stop() {
        running.set(false);
        stopWorkers();
    }

    void shutdown() {
        stop();
        executorService.shutdownNow();
    }

    private void stopWorkers() {
        running.set(false);
        for (Future<?> worker : workers) {
            worker.cancel(true);
        }
        workers.clear();
    }

    private void runWorker() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            Duration window = properties.getStatisticsWindow();
            long windowNanos = Math.max(TimeUnit.MILLISECONDS.toNanos(100), window.toNanos());
            long busyNanos = (long) (windowNanos * dutyCycle);
            long started = System.nanoTime();
            while (System.nanoTime() - started < busyNanos && running.get()) {
                Thread.onSpinWait();
            }
            long elapsed = System.nanoTime() - started;
            long sleepNanos = windowNanos - elapsed;
            if (sleepNanos > 0L) {
                try {
                    TimeUnit.NANOSECONDS.sleep(sleepNanos);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class NamedDaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "resource-pressure-cpu-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
