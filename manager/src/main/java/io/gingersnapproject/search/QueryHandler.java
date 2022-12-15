package io.gingersnapproject.search;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.gingersnapproject.Caches;
import io.gingersnapproject.configuration.Configuration;
import io.gingersnapproject.configuration.Rule;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Singleton
public class QueryHandler {
   @Inject
   Caches caches;
   @Inject
   SearchBackend searchBackend;
   @Inject
   Configuration configuration;

   public Uni<String> put(String indexName, String documentId, String jsonString) {
      Rule rule = configuration.rules().get(indexName);
      if (rule == null || !rule.queryEnabled()) {
         return Uni.createFrom().nullItem();
      }
      return searchBackend.put(indexName, documentId, jsonString);
   }

   public Uni<String> remove(String indexName, String documentId) {
      Rule rule = configuration.rules().get(indexName);
      if (rule == null || !rule.queryEnabled()) {
         return Uni.createFrom().nullItem();
      }
      return searchBackend.remove(indexName, documentId);
   }

   public Multi<String> query(String query) {
      Uni<SearchResult> result = searchBackend.query(query);
      return result.toMulti().onItem().transformToIterable(SearchResult::hits)
            .onItem().transformToUni(sh -> caches.get(sh.indexName(), sh.documentId()))
            // Should we allow concurrency?
            .concatenate();
   }
}
