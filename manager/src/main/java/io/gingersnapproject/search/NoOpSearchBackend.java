package io.gingersnapproject.search;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;

@LookupIfProperty(name = SearchBackend.PROPERTY, stringValue = "none", lookupIfMissing = true)
public class NoOpSearchBackend implements SearchBackend {
   @Override
   public Uni<String> put(String indexName, String documentId, String jsonString) {
      return Uni.createFrom().failure(new IllegalStateException("Index service is not enabled, this can be enabled by supplying a property for " + SearchBackend.PROPERTY));
   }

   @Override
   public Uni<String> remove(String indexName, String documentId) {
      return Uni.createFrom().failure(new IllegalStateException("Index service is not enabled, this can be enabled by supplying a property for " + SearchBackend.PROPERTY));
   }

   @Override
   public Uni<SearchResult> query(String sql) {
      return Uni.createFrom().failure(new IllegalStateException("Index service is not enabled, this can be enabled by supplying a property for " + SearchBackend.PROPERTY));
   }
}
