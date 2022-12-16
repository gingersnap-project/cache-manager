package io.gingersnapproject.search;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.gingersnapproject.configuration.Configuration;
import io.gingersnapproject.configuration.Rule;
import io.smallrye.mutiny.Uni;

@Singleton
public class IndexingHandler {

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
}
