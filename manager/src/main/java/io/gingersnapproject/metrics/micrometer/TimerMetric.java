package io.gingersnapproject.metrics.micrometer;

public enum TimerMetric {
    CACHE_LOCAL_HIT("cache_local_hit", "Gingersnap cache hits latency when the value is found locally"),
    CACHE_LOCAL_MISS("cache_local_miss", "Gingersnap cache miss latency when a tombstone is found locally"),
    CACHE_REMOTE_HIT("cache_hit_with_database", "Gingersnap cache hits latency when the value is fetched from the database"),
    CACHE_REMOTE_MISS("cache_miss_with_database", "Gingersnap cache miss latency when the value is not found in the database"),
    CACHE_ERROR("cache_error", "Gingersnap error latency");

    private final String metricName;
    private final String description;

    TimerMetric(String metricName, String description) {
        this.metricName = metricName;
        this.description = description;
    }

    public String metricName() {
        return metricName;
    }

    public String description() {
        return description;
    }
}
