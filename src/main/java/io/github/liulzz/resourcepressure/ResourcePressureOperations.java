package io.github.liulzz.resourcepressure;

import java.time.Duration;

/**
 * Programmatic API exposed to business services that depend on this starter.
 */
public interface ResourcePressureOperations {

    /**
     * Start applying adaptive CPU and heap pressure for the configured target that matches the supplied concurrency.
     *
     * @param concurrency business-side concurrency level used to select/interpolate a configured utilization target
     * @return current pressure status after the target has been applied
     */
    ResourcePressureStatus start(int concurrency);

    /**
     * Start applying adaptive CPU and heap pressure and stop automatically after the supplied duration.
     *
     * @param concurrency business-side concurrency level used to select/interpolate a configured utilization target
     * @param duration optional duration; null, zero, or negative means run until {@link #stop()} is called
     * @return current pressure status after the target has been applied
     */
    ResourcePressureStatus start(int concurrency, Duration duration);

    /** Stop CPU workers and release retained heap chunks. */
    ResourcePressureStatus stop();

    /** Return the latest pressure status. */
    ResourcePressureStatus status();
}
