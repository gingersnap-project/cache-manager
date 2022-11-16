package io.gingersnap.infinispan;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.core.admin.embeddedserver.EmbeddedServerAdminOperationHandler;
import org.infinispan.server.hotrod.configuration.HotRodServerConfiguration;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

/**
 * @author Ryan Emerson
 */
@Singleton
public class HotRodServer {

   @Inject
   EmbeddedCacheManager cacheManager;

   org.infinispan.server.hotrod.HotRodServer server;

   void start(@Observes StartupEvent ignore) {
      HotRodServerConfiguration build = new HotRodServerConfigurationBuilder()
            .adminOperationsHandler(new EmbeddedServerAdminOperationHandler())
            .build();
      server = new org.infinispan.server.hotrod.HotRodServer();
      System.out.println("START:"+cacheManager.hashCode());
      server.start(build, cacheManager);
   }

   void stop(@Observes ShutdownEvent ignore) {
      if (server != null) {
         server.stop();
      }
   }
}
