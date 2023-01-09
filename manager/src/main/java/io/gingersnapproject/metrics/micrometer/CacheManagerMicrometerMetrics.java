package io.gingersnapproject.metrics.micrometer;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.gingersnapproject.metrics.CacheAccessRecord;
import io.gingersnapproject.metrics.CacheManagerMetrics;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A {@link CacheManagerMetrics} implementation that uses {@link MeterRegistry} to store metrics information.
 */
public class CacheManagerMicrometerMetrics implements CacheManagerMetrics {

   public static final String COMPONENT_KEY = "gingersnap";
   public static final String COMPONENT_NAME = "cache.manager";
   public static final String RULE_KEY = "rule";

   private final MeterRegistry registry;
   private final Map<String, RuleMetrics> perRulesMetrics = new ConcurrentHashMap<>();

   public CacheManagerMicrometerMetrics(MeterRegistry registry) {
      this.registry = registry;
      // use key "gingersnap" to identify the component
      registry.config().commonTags(COMPONENT_KEY, COMPONENT_NAME);
   }

   @Override
   public <T> CacheAccessRecord<T> recordCacheAccess(String rule) {
      return new CacheAccessRecordImpl<>(rule, System.nanoTime());
   }

   @Override
   public void registerRulesMetrics(String rule, LoadingCache<?, ?> cache) {
      perRulesMetrics.computeIfAbsent(rule, k -> createRuleMetrics(k, cache));
   }

   @Override
   public void unregisterRulesMetrics(String rule) {
      var metrics = perRulesMetrics.remove(rule);
      if (metrics != null) {
         metrics.unregister(registry);
      }
   }

   private RuleMetrics createRuleMetrics(String rule, LoadingCache<?,?> cache) {
      var metrics = Arrays.stream(PerRuleGaugeMetric.values())
            .map(gauge -> gauge.registerMetric(registry, rule, cache))
            .toList();

      EnumMap<PerRuleTimerMetric, Timer> timers = new EnumMap<>(PerRuleTimerMetric.class);
      for (PerRuleTimerMetric metric : PerRuleTimerMetric.values()) {
         timers.put(metric, metric.registerMetric(registry, rule));
      }
      return new RuleMetrics(timers, metrics);
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
            recordLatency(localHit ? PerRuleTimerMetric.CACHE_LOCAL_HIT : PerRuleTimerMetric.CACHE_REMOTE_HIT);
         } else if (throwable != null) {
            recordLatency(PerRuleTimerMetric.CACHE_ERROR);
         } else {
            recordLatency(localHit ? PerRuleTimerMetric.CACHE_LOCAL_MISS : PerRuleTimerMetric.CACHE_REMOTE_MISS);
         }
      }

      @Override
      public void localHit(boolean localHit) {
         this.localHit = localHit;
      }

      private void recordLatency(PerRuleTimerMetric metric) {
         RuleMetrics metrics = perRulesMetrics.get(rule);
         if (metrics == null) {
            return;
         }
         metrics.timers.get(metric).record(System.nanoTime() - startTimeNanos, TimeUnit.NANOSECONDS);
      }
   }

   private record RuleMetrics(EnumMap<PerRuleTimerMetric, Timer> timers, List<Meter.Id> meters) {

      public void unregister(MeterRegistry registry) {
         meters.forEach(registry::remove);
         timers.values().forEach(t -> registry.remove(t.getId()));
      }
   }
}
