package io.gingersnap.infinispan;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.globalstate.ConfigurationStorage;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * @author Ryan Emerson
 */
@Singleton
public class EmbeddedCacheManagerProducer {

   final EmbeddedCacheManager cacheManager = new DefaultCacheManager(
         new GlobalConfigurationBuilder()
               .globalState()
               .enabled(true)
               .configurationStorage(ConfigurationStorage.VOLATILE)
               .build()
   );

   @Produces
   EmbeddedCacheManager cacheManager() {
      return cacheManager;
   }
}
