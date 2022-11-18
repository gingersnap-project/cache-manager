package io.gingersnapproject.infinispan.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.globalstate.ConfigurationStorage;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

@ApplicationScoped
public class EmbeddedCacheManagerProducer {

   volatile EmbeddedCacheManager cacheManager;

   @Singleton
   @Produces
   public EmbeddedCacheManager cacheManager() {
      if (cacheManager == null) {
         cacheManager = new DefaultCacheManager(
               new GlobalConfigurationBuilder()
                     .globalState()
                     .enabled(true)
                     .configurationStorage(ConfigurationStorage.VOLATILE)
                     .build()
         );
      }
      return cacheManager;
   }

   @PreDestroy
   public void destroy() {
      if (cacheManager != null) {
         cacheManager.stop();
      }
   }
}
