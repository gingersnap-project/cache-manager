package io.gingersnapproject.search;

import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Condition;
import org.infinispan.commons.dataconversion.internal.Json;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(SearchTestResource.class)
public class SearchBackendTest {

   private static final Logger LOG = Logger.getLogger(SearchBackendTest.class);

   @Inject
   SearchBackend searchBackend;

   @Test
   public void test() throws Exception {
      assertThat(searchBackend).isNotNull();

      // mapping
      String response = searchBackend.mapping("developers").await().indefinitely();
      LOG.info(response);
      assertThat(response).contains("\"index\":\"developers\"");

      Uni<String>[] unis = new Uni[100];
      for (int i = 0; i < 100; i++) {
         String id = StringUtils.leftPad(i + "", 3, "0");
         Json json = Json.object("surname", "surname " + id, "name", "name " + id, "nick", "nick" + id);
         unis[i] = searchBackend.put("developers", id + "", json);
      }

      response = unis[0].await().indefinitely();
      LOG.info(response);

      Condition<String> created = new Condition<>(r -> r.contains("\"result\":\"created\""), "created");
      Condition<String> updated = new Condition<>(r -> r.contains("\"result\":\"updated\""), "updated");
      assertThat(response).is(anyOf(created, updated));

      Uni.combine().all().unis(unis).combinedWith(unused -> null).await().indefinitely();

      // TODO find a better way to wait for the puts
      Thread.sleep(1000);

      // search
      SearchResult queryResponse = searchBackend.query("select * from developers order by name")
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
      queryResponse = searchBackend.query("select * from developers order by name limit 10 offset 10")
            .await().indefinitely();
      LOG.info(queryResponse);
      assertThat(queryResponse.hits()).hasSize(10);
      assertThat(queryResponse.hitCount()).isEqualTo(100);

      // the first value is supposed to be 010
      assertThat(queryResponse.hits().get(0).documentId()).isEqualTo("010");
      // the last value is supposed to be 019
      assertThat(queryResponse.hits().get(9).documentId()).isEqualTo("019");

      // remove
      response = searchBackend.remove("developers", "015").await().indefinitely();
      LOG.info(response);

      assertThat(response).contains("\"result\":\"deleted\"");

      // TODO find a better way to wait for the remove
      Thread.sleep(1000);

      // new paginated search with 015 missing
      queryResponse = searchBackend.query("select * from developers order by name limit 10 offset 10")
            .await().indefinitely();
      LOG.info(queryResponse);
      assertThat(queryResponse.hits()).extracting("documentId").containsExactly("010", "011", "012", "013", "014", "016", "017", "018", "019", "020");
      assertThat(queryResponse.hitCount()).isEqualTo(99);
   }
}
