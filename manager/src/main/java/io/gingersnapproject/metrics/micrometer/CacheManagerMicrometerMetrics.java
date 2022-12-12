package io.gingersnapproject.metrics.micrometer;

import java.time.Duration;
import java.util.EnumMap;
import java.util.concurrent.TimeUnit;

import io.gingersnapproject.metrics.CacheAccessRecord;
import io.gingersnapproject.metrics.CacheManagerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * A {@link CacheManagerMetrics} implementation that uses {@link MeterRegistry} to store metrics information.
 */
public class CacheManagerMicrometerMetrics implements CacheManagerMetrics {

    private final EnumMap<TimerMetric, Timer> timers = new EnumMap<>(TimerMetric.class);

    private static Timer createTimer(MeterRegistry registry, String mame, String description) {
        return Timer.builder(mame)
                .description(description)
                .tags("gingersnap", "cache_manager")
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

    public CacheManagerMicrometerMetrics(MeterRegistry registry) {
        for (TimerMetric metric : TimerMetric.values()) {
            timers.put(metric, createTimer(registry, metric.metricName(), metric.description()));
        }
    }

    @Override
    public <T> CacheAccessRecord<T> recordCacheAccess() {
        return new CacheAccessRecordImpl<>(System.nanoTime());
    }

    private class CacheAccessRecordImpl<T> implements CacheAccessRecord<T> {

        private final long startTimeNanos;
        private volatile boolean localHit = true;

        private CacheAccessRecordImpl(long startTimeNanos) {
            this.startTimeNanos = startTimeNanos;
        }

        @Override
        public void accept(T value, Throwable throwable) {
            if (value != null) {
                // most common use case first
                recordLatency(localHit ? TimerMetric.CACHE_LOCAL_HIT : TimerMetric.CACHE_REMOTE_HIT);
            } else if (throwable != null) {
                recordLatency(TimerMetric.CACHE_ERROR);
            } else {
                recordLatency(localHit ? TimerMetric.CACHE_LOCAL_MISS : TimerMetric.CACHE_REMOTE_MISS);
            }
        }

        @Override
        public void localHit(boolean localHit) {
            this.localHit = localHit;
        }

        private void recordLatency(TimerMetric metric) {
            timers.get(metric).record(System.nanoTime() - startTimeNanos, TimeUnit.NANOSECONDS);
        }
    }
}
