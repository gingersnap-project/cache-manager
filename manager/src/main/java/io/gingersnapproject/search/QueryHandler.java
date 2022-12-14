package io.gingersnapproject.search;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.gingersnapproject.Caches;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Singleton
public class QueryHandler {
   @Inject
   Caches caches;

   @Inject
   SearchBackend searchBackend;

   public Multi<String> query(String query) {
      Uni<SearchResult> result = searchBackend.query(query);
      return result.toMulti().onItem().transformToIterable(SearchResult::hits)
            .onItem().transformToUni(sh -> caches.get(sh.indexName(), sh.documentId()))
            // Should we allow concurrency?
            .concatenate();
   }
}
