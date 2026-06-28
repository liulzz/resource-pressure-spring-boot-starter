package io.github.liulzz.resourcepressure;

import java.math.BigDecimal;
import java.util.Locale;

final class PercentParser {
    private PercentParser() {
    }

    static double parsePercent(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0d;
        }
        String normalized = raw.trim()
                .replace("％", "%")
                .replace("±", "")
                .replace("+/-", "")
                .replace(" ", "")
                .toLowerCase(Locale.ROOT);
        if (normalized.endsWith("%")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return new BigDecimal(normalized).doubleValue();
    }
}
