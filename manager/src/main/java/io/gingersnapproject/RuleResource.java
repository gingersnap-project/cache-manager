package io.gingersnapproject;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;

import io.gingersnapproject.search.QueryHandler;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("/rules")
public class RuleResource {

   @Inject
   Caches maps;

   @Inject
   QueryHandler queryHandler;

   @GET
   @Operation(summary = "Retrieve a cache entry associated with the provided rule and key")
   @Path("/{rule}/{key}")
   @Produces(MediaType.APPLICATION_JSON)
   public Uni<String> get(String rule, String key) {
      return maps.get(rule, key);
   }

   @GET
   @Operation(summary = "Retrieve all cached keys associated with the provided rule")
   @Path("/{rule}")
   @Produces(MediaType.APPLICATION_JSON)
   public Multi<String> getAllKeys(String rule) {
      return Multi.createFrom().items(maps.getKeys(rule))
            // TODO: should be able to do this in a better way - technically we also need to escape the String as well
            .map(a -> "\"" + a + "\"");
   }

   @GET
   @Operation(summary = "Queries from any of the rules that are indexed")
   @Produces(MediaType.APPLICATION_JSON)
   public Multi<String> query(@QueryParam("query") String query) {
      return queryHandler.query(query);
   }
}
