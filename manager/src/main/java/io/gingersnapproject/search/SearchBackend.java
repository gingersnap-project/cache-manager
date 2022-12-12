package io.gingersnapproject.search;

import org.infinispan.commons.dataconversion.internal.Json;

import io.smallrye.mutiny.Uni;

public interface SearchBackend {

   Uni<String> mapping(String indexName);

   Uni<String> put(String indexName, String documentId, Json value);

   Uni<String> remove(String indexName, String documentId);

   Uni<SearchResult> query(String sql);

}
