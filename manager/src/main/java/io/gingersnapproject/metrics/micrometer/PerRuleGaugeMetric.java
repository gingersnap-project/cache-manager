package io.gingersnapproject.metrics.micrometer;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Micrometer Gauge metrics per rule.
 */
public enum PerRuleGaugeMetric {
   CACHE_SIZE("cache_size_%s") {
      @Override
      public Meter.Id registerRule(MeterRegistry registry, String rule, LoadingCache<?, ?> cache) {
         return Gauge.builder(metricName(rule), cache::estimatedSize)
               .description("Gingersnap cache size including the tombstones.")
               .tags("gingersnap", "cache_manager")
               .register(registry)
               .getId();
      }
   };

   private final String metricNameFormat;

   PerRuleGaugeMetric(String metricNameFormat) {
      this.metricNameFormat = metricNameFormat;
   }

   public abstract Meter.Id registerRule(MeterRegistry registry, String rule, LoadingCache<?, ?> cache);

   public String metricName(String rule) {
      return String.format(metricNameFormat, rule);
   }

}
