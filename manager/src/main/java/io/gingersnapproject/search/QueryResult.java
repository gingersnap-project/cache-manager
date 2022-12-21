package io.gingersnapproject.search;

import io.smallrye.mutiny.Multi;

public record QueryResult(long hitCount, boolean hitCountExact, Multi<String> hits, boolean hitsExacts) {
}
