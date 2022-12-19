package io.gingersnapproject.search;

import java.util.Map;

import io.smallrye.mutiny.Uni;

public interface SearchBackend {

   String PROPERTY = "service.index";

   Uni<String> put(String indexName, String documentId, String jsonString);

   Uni<String> putAll(String indexName, Map<String, String> documents);

   Uni<String> remove(String indexName, String documentId);

   Uni<SearchResult> query(String sql);

}
