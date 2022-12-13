package io.gingersnapproject.search;

public record SearchHit(String indexName, String documentId, Double score) {

}
