package io.github.liulzz.resourcepressure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ResourcePressureTargetSelectorTest {
    @Test
    void exactProfileReturnsConfiguredTarget() {
        ResourcePressureTarget target = selector().select(10);

        assertThat(target.concurrency()).isEqualTo(10);
        assertThat(target.cpuPercent()).isEqualTo(20d);
        assertThat(target.memoryPercent()).isEqualTo(40d);
        assertThat(target.fluctuationPercent()).isEqualTo(10d);
    }

    @Test
    void missingProfileInterpolatesBetweenNeighboringConcurrencyLevels() {
        ResourcePressureTarget target = selector().select(105);

        assertThat(target.cpuPercent()).isEqualTo(70d);
        assertThat(target.memoryPercent()).isEqualTo(53d);
    }

    @Test
    void outOfRangeConcurrencyUsesNearestBoundaryProfile() {
        assertThat(selector().select(1).cpuPercent()).isEqualTo(20d);
        assertThat(selector().select(300).cpuPercent()).isEqualTo(120d);
    }

    @Test
    void emptyProfilesReturnZeroTarget() {
        ResourcePressureTarget target = new ResourcePressureTargetSelector(new ResourcePressureProperties()).select(10);

        assertThat(target.cpuPercent()).isZero();
        assertThat(target.memoryPercent()).isZero();
    }

    @Test
    void exactUpperBoundaryUsesUpperProfile() {
        ResourcePressureTarget target = selector().select(200);

        assertThat(target.cpuPercent()).isEqualTo(120d);
        assertThat(target.memoryPercent()).isEqualTo(66d);
    }

    private static ResourcePressureTargetSelector selector() {
        ResourcePressureProperties properties = new ResourcePressureProperties();
        properties.setFluctuationRange("±10%");
        ResourcePressureProperties.Utilization low = new ResourcePressureProperties.Utilization();
        low.setConcurrency(10);
        low.setCpuUsage("20%");
        low.setMemoryUsage("40%");
        ResourcePressureProperties.Utilization high = new ResourcePressureProperties.Utilization();
        high.setConcurrency(200);
        high.setCpuUsage("120%");
        high.setMemoryUsage("66%");
        properties.setUtilization(List.of(high, low));
        return new ResourcePressureTargetSelector(properties);
    }
}
