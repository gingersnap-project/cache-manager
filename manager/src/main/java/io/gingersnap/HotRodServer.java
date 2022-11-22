package io.gingersnap;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.core.admin.AdminOperationsHandler;
import org.infinispan.server.core.admin.embeddedserver.CacheCreateTask;
import org.infinispan.server.core.admin.embeddedserver.CacheGetOrCreateTask;
import org.infinispan.server.core.admin.embeddedserver.CacheNamesTask;
import org.infinispan.server.core.admin.embeddedserver.CacheRemoveTask;
import org.infinispan.server.hotrod.configuration.HotRodServerConfiguration;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@Singleton
public class HotRodServer {

   @Inject
   EmbeddedCacheManager cacheManager;

   org.infinispan.server.hotrod.HotRodServer server;

   void start(@Observes StartupEvent ignore) {
      HotRodServerConfiguration build = new HotRodServerConfigurationBuilder()
            .host("0.0.0.0")
            .adminOperationsHandler(new EmbeddedServerAdminOperationHandler())
            .build();
      server = new org.infinispan.server.hotrod.HotRodServer();
      server.start(build, cacheManager);
   }

   void stop(@Observes ShutdownEvent ignore) {
      if (server != null) {
         server.stop();
      }
   }

   public boolean isLive() {
      // TODO make more useful
      return server.getTransport().isRunning();
   }

   public boolean isReady() {
      // TODO make more useful
      return server.getTransport().isRunning();
   }

   public boolean hasStarted() {
      // TODO make more useful
      return server.getTransport().isRunning();
   }

   static class EmbeddedServerAdminOperationHandler extends AdminOperationsHandler {

      public EmbeddedServerAdminOperationHandler() {
         super(
               new CacheCreateTask(),
               new CacheGetOrCreateTask(),
               new CacheNamesTask(),
               new CacheRemoveTask()
         );
      }
   }
}
