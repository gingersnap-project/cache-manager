package io.gingersnapproject.metrics.micrometer;

import java.time.Duration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import static io.gingersnapproject.metrics.micrometer.CacheManagerMicrometerMetrics.RULE_KEY;

public enum PerRuleTimerMetric {
    CACHE_LOCAL_HIT("cache.local.hit", "Gingersnap cache hits latency when the value is found locally"),
    CACHE_LOCAL_MISS("cache.local.miss", "Gingersnap cache miss latency when a tombstone is found locally"),
    CACHE_REMOTE_HIT("cache.database.hit", "Gingersnap cache hits latency when the value is fetched from the database"),
    CACHE_REMOTE_MISS("cache.database.miss", "Gingersnap cache miss latency when the value is not found in the database"),
    CACHE_ERROR("cache.error", "Gingersnap cache error latency");

    private final String metricName;
    private final String description;

    PerRuleTimerMetric(String metricName, String description) {
        this.metricName = metricName;
        this.description = description;
    }

    public String metricName() {
        return metricName;
    }

    public Timer registerMetric(MeterRegistry registry, String rule) {
        return Timer.builder(metricName)
              .description(description)
              .tags(RULE_KEY, rule)
              .publishPercentileHistogram()
              // TODO keep or allow to be configured?
              // pre-defined histogram bucket so we can see the number of request with latency <= the values below
              .serviceLevelObjectives(
                    Duration.ofMillis(1),
                    Duration.ofMillis(5),
                    Duration.ofMillis(10),
                    Duration.ofMillis(50),
                    Duration.ofMillis(100),
                    Duration.ofMillis(500))
              // TODO keep or configure?
              // By default, micromenter creates a tons of bucket with values > 1second. We don't need them.
              .maximumExpectedValue(Duration.ofSeconds(1))
              .register(registry);
    }
}
