package io.gingersnapproject;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;

@Path("/rules")
public class RuleResource {

   @Inject
   EmbeddedCacheManager cacheManager;

   @GET
   @Path("/{rule}/{key}")
   @Produces("application/json")
   public String get(String rule, String key) {
      key = String.format("%s:%s", rule, key);
      return cache(rule).get(key);
   }

   @GET
   @Path("/{rule}")
   public Collection<String> getAllKeys(String rule) {
      return cache(rule).keySet().stream().map(s -> s.replaceFirst("^" + rule + ":", "")).collect(Collectors.toList());
   }

   private Cache<String, String> cache(String rule) {
      // TODO use rule parameter when Cache per Rule created by DB Syncer
      return cacheManager.getCache(rule);
   }
}
