package io.gingersnapproject.search;

import java.util.Map;
import java.util.Optional;

import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class SearchTestResource implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

   private Optional<String> containerNetworkId;
   private ElasticsearchContainer container;

   @Override
   public void setIntegrationTestContext(DevServicesContext context) {
      containerNetworkId = context.containerNetworkId();
   }

   @Override
   public Map<String, String> start() {
      DockerImageName dockerImageName = DockerImageName.parse("opensearchproject/opensearch:2.4.0")
            .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch");

      container = new ElasticsearchContainer(dockerImageName);
      container.addEnv("discovery.type", "single-node");
      container.addEnv("plugins.security.ssl.http.enabled", "false");

      // apply the network to the container
      containerNetworkId.ifPresent(container::withNetworkMode);

      container.start();

      try {
         // After the container is started the OpenSearch server takes some time to init the security subsystem
         // TODO Find a better way to wait for it
         Thread.sleep(2000);
      } catch (InterruptedException e) {
      }

      return ImmutableMap.of(
            "quarkus.elasticsearch.hosts", container.getHttpHostAddress(),
            "quarkus.elasticsearch.username", "admin", "quarkus.elasticsearch.password", "admin");
   }

   @Override
   public void stop() {
      if (container != null) {
         container.stop();
      }
   }
}
