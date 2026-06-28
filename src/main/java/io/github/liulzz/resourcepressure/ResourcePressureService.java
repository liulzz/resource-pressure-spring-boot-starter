package io.github.liulzz.resourcepressure;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.DisposableBean;

public class ResourcePressureService implements DisposableBean {
    private final ResourcePressureProperties properties;
    private final ResourcePressureTargetSelector targetSelector;
    private final CpuPressureController cpuPressureController;
    private final MemoryPressureController memoryPressureController;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ResourcePressureTarget currentTarget = new ResourcePressureTarget(0, 0d, 0d, 0d);
    private volatile ScheduledFuture<?> loopFuture;
    private volatile ScheduledFuture<?> stopFuture;

    public ResourcePressureService(ResourcePressureProperties properties) {
        this(properties, new ThreadMxBeanCpuUsageMeter());
    }

    ResourcePressureService(ResourcePressureProperties properties, CpuUsageMeter cpuUsageMeter) {
        Objects.requireNonNull(properties, "properties must not be null");
        this.properties = properties;
        this.targetSelector = new ResourcePressureTargetSelector(properties);
        this.cpuPressureController = new CpuPressureController(properties, cpuUsageMeter);
        this.memoryPressureController = new MemoryPressureController(properties);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "resource-pressure-controller");
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized ResourcePressureStatus start(int concurrency, Duration duration) {
        if (concurrency < 1) {
            throw new IllegalArgumentException("concurrency must be greater than or equal to 1");
        }
        ResourcePressureTarget target = targetSelector.select(concurrency);
        this.currentTarget = target;
        running.set(true);
        cpuPressureController.start(target);
        memoryPressureController.adjust(target);
        scheduleControlLoop(target);
        scheduleStop(duration);
        return status();
    }

    public synchronized ResourcePressureStatus stop() {
        running.set(false);
        cancel(loopFuture);
        cancel(stopFuture);
        cpuPressureController.stop();
        memoryPressureController.release();
        return status();
    }

    public synchronized ResourcePressureStatus status() {
        return new ResourcePressureStatus(
                running.get(),
                currentTarget.concurrency(),
                currentTarget.cpuPercent(),
                currentTarget.memoryPercent(),
                cpuPressureController.measuredCpuPercent(),
                memoryPressureController.usedHeapPercent(),
                cpuPressureController.workerCount(),
                memoryPressureController.retainedChunkCount()
        );
    }

    @Override
    public void destroy() {
        stop();
        cpuPressureController.shutdown();
        scheduler.shutdownNow();
    }

    private void scheduleControlLoop(ResourcePressureTarget target) {
        cancel(loopFuture);
        long periodMillis = Math.max(100L, properties.getStatisticsWindow().toMillis());
        loopFuture = scheduler.scheduleAtFixedRate(() -> {
            if (running.get()) {
                cpuPressureController.feedback(target);
                memoryPressureController.adjust(target);
            }
        }, periodMillis, periodMillis, TimeUnit.MILLISECONDS);
    }

    private void scheduleStop(Duration duration) {
        cancel(stopFuture);
        if (duration != null && !duration.isZero() && !duration.isNegative()) {
            stopFuture = scheduler.schedule(this::stop, duration.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private static void cancel(ScheduledFuture<?> future) {
        if (future != null) {
            future.cancel(false);
        }
    }
}
