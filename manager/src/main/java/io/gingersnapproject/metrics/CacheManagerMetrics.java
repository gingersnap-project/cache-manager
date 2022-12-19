package io.gingersnapproject.metrics;

import com.github.benmanes.caffeine.cache.LoadingCache;

/**
 * Entry point to collect metrics.
 */
public interface CacheManagerMetrics {

   /**
    * Records a cache access.
    * <p>
    * The {@link CacheAccessRecord} must be used to record a single invocation.
    *
    * @param <T> The value's type.
    * @return A {@link CacheAccessRecord} instance.
    */
   <T> CacheAccessRecord<T> recordCacheAccess(String rule);

   /**
    * Register metrics associated with the {@code rule} stored in {@code cache}.
    *
    * @param rule  The rule's name.
    * @param cache The {@link LoadingCache} where the data is cached.
    */
   void registerRulesMetrics(String rule, LoadingCache<?, ?> cache);

   /**
    * Un-register the metrics associated with {@code  rule}.
    *
    * @param rule The rule's name.
    */
   void unregisterRulesMetrics(String rule);
}
