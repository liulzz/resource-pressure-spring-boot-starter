package io.github.liulzz.resourcepressure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ResourcePressureTargetSelector {
    private final ResourcePressureProperties properties;

    ResourcePressureTargetSelector(ResourcePressureProperties properties) {
        this.properties = properties;
    }

    ResourcePressureTarget select(int concurrency) {
        List<ResourcePressureProperties.Utilization> profiles = new ArrayList<>(properties.getUtilization());
        profiles.sort(Comparator.comparingInt(ResourcePressureProperties.Utilization::getConcurrency));
        double fluctuation = PercentParser.parsePercent(properties.getFluctuationRange());
        if (profiles.isEmpty()) {
            return new ResourcePressureTarget(concurrency, 0d, 0d, fluctuation);
        }
        if (profiles.size() == 1 || concurrency <= profiles.get(0).getConcurrency()) {
            return fromProfile(concurrency, profiles.get(0), fluctuation);
        }
        ResourcePressureProperties.Utilization last = profiles.get(profiles.size() - 1);
        if (concurrency >= last.getConcurrency()) {
            return fromProfile(concurrency, last, fluctuation);
        }
        for (int i = 0; i < profiles.size() - 1; i++) {
            ResourcePressureProperties.Utilization lower = profiles.get(i);
            ResourcePressureProperties.Utilization upper = profiles.get(i + 1);
            if (concurrency == lower.getConcurrency()) {
                return fromProfile(concurrency, lower, fluctuation);
            }
            if (concurrency > lower.getConcurrency() && concurrency < upper.getConcurrency()) {
                double ratio = (double) (concurrency - lower.getConcurrency())
                        / (double) (upper.getConcurrency() - lower.getConcurrency());
                double cpu = interpolate(PercentParser.parsePercent(lower.getCpuUsage()),
                        PercentParser.parsePercent(upper.getCpuUsage()), ratio);
                double memory = interpolate(PercentParser.parsePercent(lower.getMemoryUsage()),
                        PercentParser.parsePercent(upper.getMemoryUsage()), ratio);
                return new ResourcePressureTarget(concurrency, cpu, memory, fluctuation);
            }
        }
        return fromProfile(concurrency, last, fluctuation);
    }

    private static ResourcePressureTarget fromProfile(
            int requestedConcurrency,
            ResourcePressureProperties.Utilization profile,
            double fluctuation
    ) {
        return new ResourcePressureTarget(
                requestedConcurrency,
                PercentParser.parsePercent(profile.getCpuUsage()),
                PercentParser.parsePercent(profile.getMemoryUsage()),
                fluctuation
        );
    }

    private static double interpolate(double lower, double upper, double ratio) {
        return lower + (upper - lower) * ratio;
    }
}
