package io.gingersnapproject.metrics;

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
   <T> CacheAccessRecord<T> recordCacheAccess();

}
