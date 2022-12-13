package io.gingersnapproject.search.opensearch;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.infinispan.commons.dataconversion.internal.Json;

import io.gingersnapproject.search.SearchBackend;
import io.gingersnapproject.search.SearchResult;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;

@LookupIfProperty(name = "service.opensearch.enabled", stringValue = "true")
@ApplicationScoped
public class OpenSearchBackend implements SearchBackend {

   @Inject
   private RestClient restClient;

   @Override
   public Uni<String> mapping(String indexName) {
      Request request = new Request("PUT", "/" + indexName);

      Json mappings = Json.object("mappings",
            Json.object("_source",
                  Json.object("enabled", false)));
      request.setJsonEntity(mappings.toString());

      return commandSubmit(request);
   }

   @Override
   public Uni<String> put(String indexName, String documentId, Json value) {
      Request request = new Request("PUT", "/" + indexName + "/_doc/" + documentId);
      request.setJsonEntity(value.toString());

      return commandSubmit(request);
   }

   @Override
   public Uni<String> remove(String indexName, String documentId) {
      Request request = new Request("DELETE", "/" + indexName + "/_doc/" + documentId);

      return commandSubmit(request);
   }

   @Override
   public Uni<SearchResult> query(String sql) {
      Request request = new Request("POST", "/_plugins/_sql");
      request.addParameter("format", "json");
      request.setJsonEntity(Json.object("query", sql).toString());

      return submitQuery(request);
   }

   private Uni<String> commandSubmit(Request request) {
      return Uni.createFrom().emitter(em -> restClient.performRequestAsync(request, new CommandResponseListener(em)));
   }

   private Uni<SearchResult> submitQuery(Request request) {
      return Uni.createFrom().emitter(em -> restClient.performRequestAsync(request, new QueryResponseListener(em)));
   }
}
