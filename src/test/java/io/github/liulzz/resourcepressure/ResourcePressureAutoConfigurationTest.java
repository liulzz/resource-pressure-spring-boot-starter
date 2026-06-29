package io.github.liulzz.resourcepressure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ResourcePressureAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ResourcePressureAutoConfiguration.class));

    @Test
    void createsProgrammaticOperationsBeanByDefaultWithoutWebStack() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(ResourcePressureOperations.class)
                .hasSingleBean(ResourcePressureService.class)
                .doesNotHaveBean("resourcePressureController"));
    }

    @Test
    void operationsBeanCanBeInjectedAndCalledFromBusinessCode() {
        contextRunner.withPropertyValues(
                        "resource-pressure.statistics-window=100ms",
                        "resource-pressure.memory-chunk-bytes=4096",
                        "resource-pressure.utilization[0].concurrency=10",
                        "resource-pressure.utilization[0].cpu-usage=0%",
                        "resource-pressure.utilization[0].memory-usage=0%")
                .run(context -> {
                    ResourcePressureOperations operations = context.getBean(ResourcePressureOperations.class);

                    ResourcePressureStatus started = operations.start(10, Duration.ofMillis(100));
                    ResourcePressureStatus stopped = operations.stop();

                    assertThat(started.running()).isTrue();
                    assertThat(started.targetCpuPercent()).isZero();
                    assertThat(started.targetMemoryPercent()).isZero();
                    assertThat(stopped.running()).isFalse();
                });
    }

    @Test
    void backsOffWhenDisabled() {
        contextRunner.withPropertyValues("resource-pressure.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ResourcePressureOperations.class));
    }

    @Test
    void customOperationsBeanBacksOffDefaultService() {
        ResourcePressureOperations customOperations = new NoopResourcePressureOperations();
        contextRunner.withBean(ResourcePressureOperations.class, () -> customOperations)
                .run(context -> assertThat(context.getBean(ResourcePressureOperations.class)).isSameAs(customOperations));
    }

    @Test
    void customCpuUsageMeterBeanBacksOffDefault() {
        CpuUsageMeter customMeter = () -> 42d;
        contextRunner.withBean(CpuUsageMeter.class, () -> customMeter)
                .run(context -> assertThat(context.getBean(CpuUsageMeter.class)).isSameAs(customMeter));
    }

    @Test
    void bindsUtilizationYamlEquivalentProperties() {
        contextRunner.withPropertyValues(
                        "resource-pressure.fluctuation-range=±10%",
                        "resource-pressure.statistics-window=2s",
                        "resource-pressure.utilization[0].concurrency=10",
                        "resource-pressure.utilization[0].cpu-usage=20%",
                        "resource-pressure.utilization[0].memory-usage=40%")
                .run(context -> {
                    ResourcePressureProperties properties = context.getBean(ResourcePressureProperties.class);
                    assertThat(properties.getStatisticsWindow()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(properties.getUtilization()).hasSize(1);
                    assertThat(properties.getUtilization().get(0).getCpuUsage()).isEqualTo("20%");
                });
    }

    private static final class NoopResourcePressureOperations implements ResourcePressureOperations {
        @Override
        public ResourcePressureStatus start(int concurrency) {
            return status();
        }

        @Override
        public ResourcePressureStatus start(int concurrency, Duration duration) {
            return status();
        }

        @Override
        public ResourcePressureStatus stop() {
            return status();
        }

        @Override
        public ResourcePressureStatus status() {
            return new ResourcePressureStatus(false, 0, 0d, 0d, 0d, 0d, 0, 0);
        }
    }
}
