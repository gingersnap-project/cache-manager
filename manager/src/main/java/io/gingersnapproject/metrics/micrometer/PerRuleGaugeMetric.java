package io.gingersnapproject.metrics.micrometer;

import java.util.function.Function;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

import static io.gingersnapproject.metrics.micrometer.CacheManagerMicrometerMetrics.RULE_KEY;

/**
 * Micrometer Gauge metrics per rule.
 */
public enum PerRuleGaugeMetric {
   CACHE_SIZE("cache.size", "Gingersnap cache size including the tombstones.", LoadingCache::estimatedSize);

   private final String metricName;
   private final String description;
   private final Function<LoadingCache<?,?>, Number> numberSupplier;

   PerRuleGaugeMetric(String metricName, String description, Function<LoadingCache<?,?>, Number> numberSupplier) {
      this.metricName = metricName;
      this.description = description;
      this.numberSupplier = numberSupplier;
   }

   public Meter.Id registerMetric(MeterRegistry registry, String rule, LoadingCache<?, ?> cache) {
      return Gauge.builder(metricName, () -> numberSupplier.apply(cache))
            .description(description)
            .tag(RULE_KEY, rule)
            .register(registry)
            .getId();
   }

   public String metricName() {
      return metricName;
   }

}
