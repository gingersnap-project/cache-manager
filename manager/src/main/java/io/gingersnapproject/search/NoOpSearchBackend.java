package io.gingersnapproject.search;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
@LookupIfProperty(name = SearchBackend.PROPERTY, stringValue = "none", lookupIfMissing = true)
public class NoOpSearchBackend implements SearchBackend {

   @Override
   public Uni<String> put(String indexName, String documentId, String jsonString) {
      return unsupported();
   }

   @Override
   public Uni<String> putAll(String indexName, Map<String, String> documents) {
      return unsupported();
   }

   @Override
   public Uni<String> remove(String indexName, String documentId) {
      return unsupported();
   }

   @Override
   public Uni<SearchResult> query(String sql) {
      return unsupported();
   }

   public <T> Uni<T> unsupported() {
      return Uni.createFrom().failure(new IllegalStateException("Index service is not enabled, this can be enabled by supplying a property for " + SearchBackend.PROPERTY));
   }
}
