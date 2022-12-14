package io.gingersnapproject.search;

import io.smallrye.mutiny.Uni;

public interface SearchBackend {

   String PROPERTY = "service.index";

   Uni<String> mapping(String indexName);

   Uni<String> put(String indexName, String documentId, String jsonString);

   Uni<String> remove(String indexName, String documentId);

   Uni<SearchResult> query(String sql);

}
