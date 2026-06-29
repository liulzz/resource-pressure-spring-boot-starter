package io.github.liulzz.resourcepressure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(ResourcePressureProperties.class)
@ConditionalOnProperty(prefix = "resource-pressure", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ResourcePressureAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CpuUsageMeter cpuUsageMeter() {
        return new ThreadMxBeanCpuUsageMeter();
    }

    @Bean
    @ConditionalOnMissingBean(ResourcePressureOperations.class)
    public ResourcePressureService resourcePressureService(ResourcePressureProperties properties, CpuUsageMeter cpuUsageMeter) {
        return new ResourcePressureService(properties, cpuUsageMeter);
    }
}
