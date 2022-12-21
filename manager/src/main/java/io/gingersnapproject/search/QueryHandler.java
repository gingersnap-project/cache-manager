package io.gingersnapproject.search;

import javax.enterprise.inject.Instance;
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
   Instance<SearchBackend> searchBackend;

   public Uni<QueryResult> query(String query) {
      Uni<SearchResult> result = searchBackend.get().query(query);
      final Multi<String> hits = result.toMulti().onItem().transformToIterable(SearchResult::hits)
            .onItem().transformToUni(sh -> caches.get(sh.indexName(), sh.documentId()))
            // Should we allow concurrency?
            .concatenate();

      return result
            .onItem()
            .transform(searchResult -> new QueryResult(searchResult.hitCount(), searchResult.hitCountExact(),
                  hits, searchResult.hitsExact()));
   }
}
