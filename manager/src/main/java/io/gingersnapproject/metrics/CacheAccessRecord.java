package io.gingersnapproject.metrics;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

/**
 * Records cache access latency.
 * <p>
 * This interface extends {@link BiConsumer} allowing to use with {@link CompletionStage#whenComplete(BiConsumer)}
 * methods.
 * <p>
 * Exceptions are not an expected outcome and they are recorded as errors. A {@code null} value means a cache miss.
 */
public interface CacheAccessRecord<T> extends BiConsumer<T, Throwable> {

    default void recordThrowable(Throwable throwable) {
        accept(null, throwable);
    }

    void localHit(boolean localHit);
}
