package io.gingersnapproject.search.opensearch;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.infinispan.commons.dataconversion.internal.Json;

import io.gingersnapproject.search.SearchResult;

class QueryResponseListener implements ResponseListener {

   final CompletableFuture<SearchResult> future = new CompletableFuture<>();

   @Override
   public void onSuccess(Response response) {
      try {
         String jsonList = EntityUtils.toString(response.getEntity());
         Json queryResponse = Json.read(jsonList);
         future.complete(new OpenSearchResult(queryResponse));
      } catch (Throwable throwable) {
         future.completeExceptionally(throwable);
      }
   }

   @Override
   public void onFailure(Exception exception) {
      future.completeExceptionally(exception);
   }

   public CompletionStage<SearchResult> completionStage() {
      return future;
   }
}
