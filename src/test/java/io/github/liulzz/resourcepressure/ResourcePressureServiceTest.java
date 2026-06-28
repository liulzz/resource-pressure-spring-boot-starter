package io.github.liulzz.resourcepressure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ResourcePressureServiceTest {
    @Test
    void startSelectsConfiguredTargetAndStopReleasesState() {
        ResourcePressureProperties properties = new ResourcePressureProperties();
        properties.setStatisticsWindow(Duration.ofMillis(100));
        properties.setMemoryChunkBytes(4096);
        ResourcePressureProperties.Utilization utilization = new ResourcePressureProperties.Utilization();
        utilization.setConcurrency(10);
        utilization.setCpuUsage("0%");
        utilization.setMemoryUsage("0%");
        properties.setUtilization(java.util.List.of(utilization));

        ResourcePressureService service = new ResourcePressureService(properties, () -> 0d);
        try {
            ResourcePressureStatus started = service.start(10, Duration.ofMillis(200));

            assertThat(started.running()).isTrue();
            assertThat(started.concurrency()).isEqualTo(10);
            assertThat(started.targetCpuPercent()).isEqualTo(0d);
            assertThat(started.targetMemoryPercent()).isEqualTo(0d);

            ResourcePressureStatus stopped = service.stop();
            assertThat(stopped.running()).isFalse();
            assertThat(stopped.retainedMemoryChunks()).isZero();
        } finally {
            service.destroy();
        }
    }

    @Test
    void rejectsInvalidConcurrency() {
        ResourcePressureService service = new ResourcePressureService(new ResourcePressureProperties(), () -> 0d);
        try {
            assertThatThrownBy(() -> service.start(0, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("concurrency");
        } finally {
            service.destroy();
        }
    }
}
