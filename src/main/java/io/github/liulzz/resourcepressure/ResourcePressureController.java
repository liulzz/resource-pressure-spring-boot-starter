package io.github.liulzz.resourcepressure;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("${resource-pressure.endpoint-base-path:/resource-pressure}")
public class ResourcePressureController {
    private static final Pattern SIMPLE_DURATION = Pattern.compile("^(\\d+)(ms|s|m|h|d)$", Pattern.CASE_INSENSITIVE);
    private final ResourcePressureService resourcePressureService;

    public ResourcePressureController(ResourcePressureService resourcePressureService) {
        this.resourcePressureService = resourcePressureService;
    }

    @PostMapping("/start")
    public ResponseEntity<ResourcePressureStatus> start(
            @RequestParam int concurrency,
            @RequestParam(required = false) String duration
    ) {
        if (concurrency < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "concurrency must be greater than or equal to 1");
        }
        return ResponseEntity.accepted().body(resourcePressureService.start(concurrency, parseDuration(duration)));
    }

    @PostMapping("/stop")
    public ResourcePressureStatus stop() {
        return resourcePressureService.stop();
    }

    @GetMapping("/status")
    public ResourcePressureStatus status() {
        return resourcePressureService.status();
    }

    private static Duration parseDuration(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("p")) {
            return Duration.parse(raw.trim());
        }
        Matcher matcher = SIMPLE_DURATION.matcher(normalized);
        if (!matcher.matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "duration must be ISO-8601 or a simple value such as 500ms, 30s, 5m, 1h, 1d");
        }
        long amount = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2)) {
            case "ms" -> Duration.ofMillis(amount);
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported duration unit");
        };
    }
}
