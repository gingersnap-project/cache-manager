package io.gingersnapproject.search;

import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.gingersnapproject.configuration.Configuration;
import io.gingersnapproject.configuration.Rule;
import io.smallrye.mutiny.Uni;

@Singleton
public class IndexingHandler {

   @Inject
   Instance<SearchBackend> searchBackend;

   @Inject
   Configuration configuration;

   public Uni<String> put(String indexName, String documentId, String jsonString) {
      Rule rule = configuration.rules().get(indexName);
      if (rule == null || !rule.queryEnabled()) {
         return Uni.createFrom().nullItem();
      }
      return searchBackend.get().put(indexName, documentId, jsonString);
   }

   public Uni<String> putAll(String indexName, Map<String, String> documents) {
      Rule rule = configuration.rules().get(indexName);
      if (rule == null || !rule.queryEnabled()) {
         return Uni.createFrom().nullItem();
      }
      return searchBackend.get().putAll(indexName, documents);
   }

   public Uni<String> remove(String indexName, String documentId) {
      Rule rule = configuration.rules().get(indexName);
      if (rule == null || !rule.queryEnabled()) {
         return Uni.createFrom().nullItem();
      }
      return searchBackend.get().remove(indexName, documentId);
   }
}
