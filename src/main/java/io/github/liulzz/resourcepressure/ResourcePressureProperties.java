package io.github.liulzz.resourcepressure;

import java.time.Duration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "resource-pressure")
public class ResourcePressureProperties {

    /** Enable auto configuration. */
    private boolean enabled = true;

    /** Allowed target fluctuation, e.g. 10% means ±10%. */
    private String fluctuationRange = "10%";

    /** CPU feedback/statistics window. */
    private Duration statisticsWindow = Duration.ofSeconds(2);

    /** Max heap ratio this starter is allowed to retain. */
    @DecimalMin("0.0")
    @DecimalMax("0.98")
    private double maxHeapAllocationRatio = 0.90d;

    /** Memory allocation chunk size in bytes. */
    @Min(4096)
    private int memoryChunkBytes = 1024 * 1024;

    /** Utilization profiles keyed by observed request concurrency. */
    @Valid
    private List<Utilization> utilization = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFluctuationRange() {
        return fluctuationRange;
    }

    public void setFluctuationRange(String fluctuationRange) {
        this.fluctuationRange = fluctuationRange;
    }

    public Duration getStatisticsWindow() {
        return statisticsWindow;
    }

    public void setStatisticsWindow(Duration statisticsWindow) {
        this.statisticsWindow = statisticsWindow;
    }

    public double getMaxHeapAllocationRatio() {
        return maxHeapAllocationRatio;
    }

    public void setMaxHeapAllocationRatio(double maxHeapAllocationRatio) {
        this.maxHeapAllocationRatio = maxHeapAllocationRatio;
    }

    public int getMemoryChunkBytes() {
        return memoryChunkBytes;
    }

    public void setMemoryChunkBytes(int memoryChunkBytes) {
        this.memoryChunkBytes = memoryChunkBytes;
    }

    public List<Utilization> getUtilization() {
        return utilization;
    }

    public void setUtilization(List<Utilization> utilization) {
        this.utilization = utilization == null ? new ArrayList<>() : new ArrayList<>(utilization);
        this.utilization.sort(Comparator.comparingInt(Utilization::getConcurrency));
    }

    public static class Utilization {
        @Min(1)
        private int concurrency;
        private String cpuUsage = "0%";
        private String memoryUsage = "0%";

        public int getConcurrency() {
            return concurrency;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        public String getCpuUsage() {
            return cpuUsage;
        }

        public void setCpuUsage(String cpuUsage) {
            this.cpuUsage = cpuUsage;
        }

        public String getMemoryUsage() {
            return memoryUsage;
        }

        public void setMemoryUsage(String memoryUsage) {
            this.memoryUsage = memoryUsage;
        }
    }
}
