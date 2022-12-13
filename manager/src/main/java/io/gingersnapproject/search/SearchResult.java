package io.gingersnapproject.search;

import java.time.Duration;
import java.util.List;

public interface SearchResult {

   long hitCount();

   boolean hitCountExact();

   List<SearchHit> hits();

   boolean hitsExact();

   Duration duration();

}
