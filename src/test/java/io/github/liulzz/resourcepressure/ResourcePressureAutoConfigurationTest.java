package io.github.liulzz.resourcepressure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ResourcePressureAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ResourcePressureAutoConfiguration.class));

    @Test
    void createsServiceAndControllerByDefault() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(ResourcePressureService.class)
                .hasSingleBean(ResourcePressureController.class));
    }

    @Test
    void backsOffWhenDisabled() {
        contextRunner.withPropertyValues("resource-pressure.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ResourcePressureService.class));
    }

    @Test
    void endpointCanBeDisabledWhileServiceStaysAvailable() {
        contextRunner.withPropertyValues("resource-pressure.endpoint-enabled=false")
                .run(context -> assertThat(context)
                        .hasSingleBean(ResourcePressureService.class)
                        .doesNotHaveBean(ResourcePressureController.class));
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
                    assertThat(properties.getStatisticsWindow()).isEqualTo(java.time.Duration.ofSeconds(2));
                    assertThat(properties.getUtilization()).hasSize(1);
                    assertThat(properties.getUtilization().get(0).getCpuUsage()).isEqualTo("20%");
                });
    }
}
