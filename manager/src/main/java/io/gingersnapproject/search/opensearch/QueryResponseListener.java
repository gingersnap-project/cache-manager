package io.gingersnapproject.search.opensearch;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.infinispan.commons.dataconversion.internal.Json;

import io.gingersnapproject.search.SearchResult;
import io.smallrye.mutiny.subscription.UniEmitter;

record QueryResponseListener(UniEmitter<? super SearchResult> emitter) implements ResponseListener {

   @Override
   public void onSuccess(Response response) {
      try {
         String jsonList = EntityUtils.toString(response.getEntity());
         Json queryResponse = Json.read(jsonList);
         emitter.complete(new OpenSearchResult(queryResponse));
      } catch (Throwable throwable) {
         emitter.fail(throwable);
      }
   }

   @Override
   public void onFailure(Exception exception) {
      emitter.fail(exception);
   }
}
