package io.gingersnapproject.metrics.micrometer;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.gingersnapproject.metrics.CacheAccessRecord;
import io.gingersnapproject.metrics.CacheManagerMetrics;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A {@link CacheManagerMetrics} implementation that uses {@link MeterRegistry} to store metrics information.
 */
public class CacheManagerMicrometerMetrics implements CacheManagerMetrics {
   private final MeterRegistry registry;
   private final Map<String, RuleMetrics> perRulesMetrics = new ConcurrentHashMap<>();

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
      this.registry = registry;
   }

   @Override
   public <T> CacheAccessRecord<T> recordCacheAccess(String rule) {
      return new CacheAccessRecordImpl<>(rule, System.nanoTime());
   }

   @Override
   public void registerRulesMetrics(String rule, LoadingCache<?, ?> cache) {
      perRulesMetrics.computeIfAbsent(rule, k -> {
         RuleMetrics rm = new RuleMetrics(k);
         rm.register(cache);
         return rm;
      });
   }

   @Override
   public void unregisterRulesMetrics(String rule) {
      var metrics = perRulesMetrics.remove(rule);
      if (metrics != null) {
         metrics.unregister();
      }
   }

   private class CacheAccessRecordImpl<T> implements CacheAccessRecord<T> {
      private final String rule;
      private final long startTimeNanos;
      private volatile boolean localHit = true;

      private CacheAccessRecordImpl(String rule, long startTimeNanos) {
         this.rule = rule;
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
         Objects.requireNonNull(perRulesMetrics.get(rule), String.format("Rule '%s' not registered", rule))
               .timers.get(metric).record(System.nanoTime() - startTimeNanos, TimeUnit.NANOSECONDS);
      }
   }

   private class RuleMetrics {
      private final String rule;
      private final EnumMap<TimerMetric, Timer> timers = new EnumMap<>(TimerMetric.class);
      private final List<Meter.Id> meters = new ArrayList<>();

      private RuleMetrics(String rule) {
         this.rule = rule;
      }

      public void register(LoadingCache<?, ?> cache) {
         var metrics = Arrays.stream(PerRuleGaugeMetric.values())
               .map(gauge -> gauge.registerRule(registry, rule, cache))
               .toList();

         meters.addAll(metrics);
         for (TimerMetric metric : TimerMetric.values()) {
            timers.put(metric, createTimer(registry, metric.metricName(rule), metric.description()));
         }
      }

      public void unregister() {
         meters.forEach(registry::remove);
         timers.values().forEach(t -> registry.remove(t.getId()));
      }
   }
}
