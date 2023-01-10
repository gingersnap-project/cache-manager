package io.gingersnapproject.hotrod;

import io.gingersnapproject.database.DatabaseResourcesLifecyleManager;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.dataconversion.MediaType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(value = DatabaseResourcesLifecyleManager.class)
public class HotRodOperationsTest {
   private static final String RULE_NAME = "developers-4";
   private RemoteCacheManager cm;
   private RemoteCache<String, String> cache;

   @BeforeEach
   public void beforeEach() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.addServer()
            .host("127.0.0.1")
            .port(11222);

      cm = new RemoteCacheManager(builder.build());
      cache = cm.getCache(RULE_NAME)
            .withDataFormat(DataFormat.builder().keyType(MediaType.TEXT_PLAIN).valueType(MediaType.TEXT_PLAIN).build());
   }

   @AfterEach
   public void afterEach() {
      if (cm != null) {
         cm.stop();
      }

      cm = null;
      cache = null;
   }

   @Test
   public void testGetAll() {
      Map<String, String> values = new HashMap<>();
      values.put("key1", "value1");
      values.put("key3", "value3");
      values.put("key2", "value3");

      cache.put("key1", "value1");
      cache.put("key3", "value3");
      cache.put("key2", "value3");

      assertThat(cache.getAll(values.keySet())).isEqualTo(values);
   }
}
