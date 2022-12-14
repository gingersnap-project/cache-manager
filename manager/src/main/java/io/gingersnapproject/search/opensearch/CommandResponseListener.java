package io.gingersnapproject.search.opensearch;

import java.io.IOException;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;

import io.smallrye.mutiny.subscription.UniEmitter;

record CommandResponseListener(UniEmitter<? super String> emitter) implements ResponseListener {

   @Override
   public void onSuccess(Response response) {
      try {
         String entity = EntityUtils.toString(response.getEntity());
         emitter.complete(entity);
      } catch (IOException exception) {
         // in this case the command has been executed server side,
         // the only problem here is to get the response message from the server
         emitter.complete(exception.getMessage());
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
         emitter.complete(exception.getMessage());
      } else {
         emitter.fail(exception);
      }
   }
}
