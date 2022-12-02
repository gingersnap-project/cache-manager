package io.gingersnapproject.search.opensearch;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;

class CommandResponseListener implements ResponseListener {

   final CompletableFuture<String> future = new CompletableFuture<>();

   @Override
   public void onSuccess(Response response) {
      try {
         String entity = EntityUtils.toString(response.getEntity());
         future.complete(entity);
      } catch (IOException exception) {
         // in this case the command has been executed server side,
         // the only problem here is to get the response message from the server
         future.complete(exception.getMessage());
      }
   }

   @Override
   public void onFailure(Exception exception) {
      if (exception instanceof ResponseException) {
         /*
          * The client tries to guess what's an error and what's not, but it's too naive.
          * A 404 on DELETE should be accepted for instance to support idempotency.
          */
         // TODO Verify, case by case, after the driver is integrated, the ResponseException kinds we want tolerate,
         //   completing the future normally, and the ones we don't want, reporting the exception to the caller,
         //   completing the future exceptionally.
         future.complete(exception.getMessage());
      }
      future.completeExceptionally(exception);
   }

   public CompletionStage<String> completionStage() {
      return future;
   }
}
