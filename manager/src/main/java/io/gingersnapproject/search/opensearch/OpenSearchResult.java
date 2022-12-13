package io.gingersnapproject.search.opensearch;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.infinispan.commons.dataconversion.internal.Json;

import io.gingersnapproject.search.SearchHit;
import io.gingersnapproject.search.SearchResult;

public class OpenSearchResult implements SearchResult {

   private final long hitCount;
   private final boolean hitCountExact;
   private final List<SearchHit> hits;
   private final boolean hitsExacts;
   private final Duration duration;

   public OpenSearchResult(Json queryResponse) {
      long took = queryResponse.at("took").asLong();
      duration = Duration.ofMillis(took);
      hitsExacts = !queryResponse.at("timed_out").asBoolean();
      Json hits = queryResponse.at("hits");
      Json total = hits.at("total");
      hitCount = total.at("value").asLong();
      String countRelation = total.at("relation").asString();
      hitCountExact = "EQ".equalsIgnoreCase(countRelation);
      this.hits = hits.at("hits").asJsonList().stream().map((hit) -> {
         String indexName = hit.at("_index").asString();
         String documentId = hit.at("_id").asString();
         Json jsonScore = hit.at("_score");
         Double score = (jsonScore.isNull()) ? null : jsonScore.asDouble();
         return new SearchHit(indexName, documentId, score);
      }).collect(Collectors.toList());
   }

   @Override
   public long hitCount() {
      return hitCount;
   }

   @Override
   public boolean hitCountExact() {
      return hitCountExact;
   }

   @Override
   public List<SearchHit> hits() {
      return hits;
   }

   @Override
   public boolean hitsExact() {
      return hitsExacts;
   }

   @Override
   public Duration duration() {
      return duration;
   }

   @Override
   public String toString() {
      return "OpenSearchResult{" +
            "hitCount=" + hitCount +
            ", hitCountExact=" + hitCountExact +
            ", hits=" + hits +
            ", hitsExacts=" + hitsExacts +
            ", duration=" + duration +
            '}';
   }
}
