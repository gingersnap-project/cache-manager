package io.gingersnapproject.metrics.micrometer;

import io.gingersnapproject.metrics.CacheManagerMetrics;
import io.micrometer.core.instrument.MeterRegistry;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

/**
 * Produces a {@link CacheManagerMetrics} implementation.
 * <p>
 * Current implementation uses MicroMeter to store and report metrics information.
 */
@ApplicationScoped
public class CacheManagerMetricsProducer {

   private final CacheManagerMetrics metrics;

   public CacheManagerMetricsProducer(MeterRegistry registry) {
      metrics = new CacheManagerMicrometerMetrics(registry);
   }

   @Produces
   public CacheManagerMetrics cacheManagerMetrics() {
      return metrics;
   }

}
