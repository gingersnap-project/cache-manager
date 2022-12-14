package io.gingersnapproject.search;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.infinispan.commons.dataconversion.internal.Json;
import org.junit.jupiter.api.Test;

import io.gingersnapproject.Caches;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(SearchTestResource.class)
public class CachesTest {
   private static final String INDEX_NAME = "developers-2";
   @Inject
   Caches caches;

   @Inject
   SearchBackend searchBackend;

   @Test
   public void testPutAndRemove() throws InterruptedException {
      Json originalJohn = Json.object("surname", "Doo", "name", "John", "nick", "john");
      Json originalMike = Json.object("surname", "Lee", "name", "Mike", "nick", "mike");

      caches.put(INDEX_NAME, "john", originalJohn.toString()).await().indefinitely();
      caches.put(INDEX_NAME, "mike", originalMike.toString()).await().indefinitely();

      Json reloadedJohn = Json.read(caches.get(INDEX_NAME, "john").await().indefinitely());
      Json reloadedMike = Json.read(caches.get(INDEX_NAME, "mike").await().indefinitely());
      assertThat(originalJohn).isEqualTo(reloadedJohn);
      assertThat(originalMike).isEqualTo(reloadedMike);

      Thread.sleep(2000);

      SearchResult queryResponse = searchBackend.query("select * from " + INDEX_NAME + " order by name")
            .await().indefinitely();

      assertThat(queryResponse.hitCount()).isEqualTo(2L);
      assertThat(queryResponse.hitCountExact()).isEqualTo(true);
      assertThat(queryResponse.hits()).extracting("documentId").containsExactly("john", "mike");
      assertThat(queryResponse.hitsExact()).isTrue();

      caches.remove(INDEX_NAME, "john").await().indefinitely();

      assertThat(caches.get(INDEX_NAME, "john").await().indefinitely()).isNull();

      Thread.sleep(2000);

      queryResponse = searchBackend.query("select * from " + INDEX_NAME + " order by name")
            .await().indefinitely();

      assertThat(queryResponse.hitCount()).isEqualTo(1L);
      assertThat(queryResponse.hitCountExact()).isEqualTo(true);
      assertThat(queryResponse.hits()).extracting("documentId").containsExactly("mike");
      assertThat(queryResponse.hitsExact()).isTrue();
   }
}
