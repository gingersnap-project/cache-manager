package io.gingersnapproject.mutiny;

import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

/**
 * This class is essentially copied from UniCreateFromKnownItem but exposes the item to allow us to retrieve it
 * without subscribing
 * @param <T>
 */
public class UniItem<T> extends AbstractUni<T> {

   private final T item;

   private UniItem(T item) {
      this.item = item;
   }

   public static <T> UniItem<T> fromItem(T item) {
      return new UniItem<>(item);
   }

   public T getItem() {
      return item;
   }

   @Override
   public void subscribe(UniSubscriber<? super T> subscriber) {
      new KnownItemSubscription(subscriber).forward();
   }

   private class KnownItemSubscription implements UniSubscription {

      private final UniSubscriber<? super T> subscriber;
      private volatile boolean cancelled = false;

      private KnownItemSubscription(UniSubscriber<? super T> subscriber) {
         this.subscriber = subscriber;
      }

      private void forward() {
         subscriber.onSubscribe(this);
         if (!cancelled) {
            subscriber.onItem(item);
         }
      }

      @Override
      public void cancel() {
         cancelled = true;
      }
   }
}
