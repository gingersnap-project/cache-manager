package io.gingersnapproject.search;

import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Condition;
import org.infinispan.commons.dataconversion.internal.Json;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.gingersnapproject.search.opensearch.OpenSearchBackend;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(SearchTestResource.class)
public class SearchBackendTest {

   private static final Logger LOG = Logger.getLogger(SearchBackendTest.class);
   private static final String INDEX_NAME = "developers-1";

   @Inject
   OpenSearchBackend searchBackend;

   @Test
   public void test() throws Exception {
      assertThat(searchBackend).isNotNull();

      // mapping
      String response = searchBackend.mapping(INDEX_NAME).await().indefinitely();
      LOG.info(response);
      assertThat(response).contains("\"index\":\"" + INDEX_NAME + "\"");

      HashMap<String, String> documents = new HashMap<>();
      for (int i = 0; i < 100; i++) {
         String id = StringUtils.leftPad(i + "", 3, "0");
         String jsonString = Json.object("surname", "surname " + id, "name", "name " + id, "nick", "nick" + id)
               .toPrettyString(); // using here a multiple lines JSON format
         documents.put(id, jsonString);
      }
      response = searchBackend.putAll(INDEX_NAME, documents).await().indefinitely();

      LOG.info(response);

      Condition<String> created = new Condition<>(r -> r.contains("\"result\":\"created\""), "created");
      Condition<String> updated = new Condition<>(r -> r.contains("\"result\":\"updated\""), "updated");
      assertThat(response).is(anyOf(created, updated));

      // search
      SearchResult queryResponse = searchBackend.query("select * from " + INDEX_NAME + " order by name")
            .await().indefinitely();
      LOG.info(queryResponse);
      assertThat(queryResponse.hits()).hasSize(100);
      assertThat(queryResponse.hitsExact()).isTrue();
      assertThat(queryResponse.hitCount()).isEqualTo(100);
      assertThat(queryResponse.hitCountExact()).isTrue();
      assertThat(queryResponse.duration()).isPositive();

      // the eleventh value is supposed to be 010
      assertThat(queryResponse.hits().get(10).documentId()).isEqualTo("010");
      // the twentieth value is supposed to be 019
      assertThat(queryResponse.hits().get(19).documentId()).isEqualTo("019");

      // paginated search
      queryResponse = searchBackend.query("select * from " + INDEX_NAME + " order by name limit 10 offset 10")
            .await().indefinitely();
      LOG.info(queryResponse);
      assertThat(queryResponse.hits()).hasSize(10);
      assertThat(queryResponse.hitCount()).isEqualTo(100);

      // the first value is supposed to be 010
      assertThat(queryResponse.hits().get(0).documentId()).isEqualTo("010");
      // the last value is supposed to be 019
      assertThat(queryResponse.hits().get(9).documentId()).isEqualTo("019");

      // remove
      response = searchBackend.remove(INDEX_NAME, "015").await().indefinitely();
      LOG.info(response);

      assertThat(response).contains("\"result\":\"deleted\"");

      // TODO find a better way to wait for the remove
      Thread.sleep(1000);

      // new paginated search with 015 missing
      queryResponse = searchBackend.query("select * from " + INDEX_NAME + " order by name limit 10 offset 10")
            .await().indefinitely();
      LOG.info(queryResponse);
      assertThat(queryResponse.hits()).extracting("documentId").containsExactly("010", "011", "012", "013", "014", "016", "017", "018", "019", "020");
      assertThat(queryResponse.hitCount()).isEqualTo(99);
   }
}
