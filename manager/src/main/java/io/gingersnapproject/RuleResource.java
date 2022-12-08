package io.gingersnapproject;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;

import io.smallrye.mutiny.Multi;

@Path("/rules")
public class RuleResource {

   @Inject
   Caches maps;

   @GET
   @Operation(summary = "Retrieve a cache entry associated with the provided rule and key")
   @Path("/{rule}/{key}")
   @Produces(MediaType.APPLICATION_JSON)
   public String get(String rule, String key) {
      byte[] value = maps.get(rule, key);
      return value == null ? null : new String(value);
   }

   @GET
   @Operation(summary = "Retrieve all cached entries associated with the provided rule")
   @Path("/{rule}")
   @Produces(MediaType.APPLICATION_JSON)
   public Multi<String> getAllKeys(String rule) {
      return Multi.createFrom().items(maps.getKeys(rule))
            // TODO: should be able to do this in a better way - technically we also need to escape the String as well
            .map(a -> "\"" + a + "\"");
   }
}
