package io.gingersnapproject.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.infinispan.commons.dataconversion.internal.Json;
import org.junit.jupiter.api.Test;

import io.gingersnapproject.Caches;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(SearchTestResource.class)
public class CachesAndQueryTest {
   private static final String INDEX_NAME = "developers-3";
   @Inject
   Caches caches;

   @Inject
   QueryHandler queryHandler;

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

      List<String> results = queryHandler.query("select * from " + INDEX_NAME + " order by name")
            .subscribe().asStream().collect(Collectors.toList());

      assertThat(results.size()).isEqualTo(2L);
      assertThat(results).containsExactly(originalJohn.toString(), originalMike.toString());

      caches.remove(INDEX_NAME, "john").await().indefinitely();

      assertThat(caches.get(INDEX_NAME, "john").await().indefinitely()).isNull();

      Thread.sleep(2000);

      results = queryHandler.query("select * from " + INDEX_NAME + " order by name")
            .subscribe().asStream().collect(Collectors.toList());

      assertThat(results.size()).isEqualTo(1L);
      assertThat(results).containsExactly(originalMike.toString());
   }
}