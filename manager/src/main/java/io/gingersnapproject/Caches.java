package io.gingersnapproject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import io.gingersnapproject.configuration.Configuration;
import io.gingersnapproject.database.DatabaseHandler;
import io.gingersnapproject.metrics.CacheAccessRecord;
import io.gingersnapproject.metrics.CacheManagerMetrics;
import io.gingersnapproject.mutiny.UniItem;
import io.gingersnapproject.search.SearchBackend;
import io.smallrye.mutiny.Uni;

@Singleton
public class Caches {
   @Inject
   CacheManagerMetrics metrics;
   private final ConcurrentMap<String, LoadingCache<String, Uni<String>>> maps = new ConcurrentHashMap<>();

   @Inject
   DatabaseHandler databaseHandler;
   @Inject
   Configuration configuration;
   @Inject
   SearchBackend searchBackend;

   private LoadingCache<String, Uni<String>> getOrCreateMap(String name) {
      if (!configuration.rules().containsKey(name)) {
         throw new IllegalArgumentException("Rule " + name + " not configured");
      }
      return maps.computeIfAbsent(name, ___ -> Caffeine.newBuilder()
            // TODO: populate this with config
            .maximumWeight(1_000_000)
            .<String, Uni<String>>weigher((k, v) -> {
               // TODO: need to revisit this later
               int size = k.length() * 2 + 1;
               if (v instanceof UniItem<String> uniItem) {
                  var actualValue = uniItem.getItem();
                  size += actualValue == null ? 0 : actualValue.length() * 2 + 1;
                  if (size < 0) {
                     size = Integer.MAX_VALUE;
                  }
               }
               return size;
            })
            .build(key -> {
               Uni<String> dbUni = databaseHandler.select(name, key)
                     // Make sure to use memoize, so that if multiple people subscribe to this it won't cause
                     // multiple DB lookups
                     .memoize().indefinitely();
               // This will replace the pending Uni from the DB with a UniItem so we can properly size the entry
               // Note due to how lazy subscribe works the entry won't be present in the map  yet
               // TODO: technically only want to do this on caller subscribing
               dbUni.subscribe()
                     .with(result -> replace(name, key, dbUni, result), t -> actualRemove(name, key, dbUni));
               return dbUni;
            }));
   }

   private void replace(String name, String key, Uni<String> prev, String value) {
      var cache = maps.get(name);
      if (cache != null) {
         cache.asMap().replace(key, prev, UniItem.fromItem(value));
      }
   }

   private void actualRemove(String name, String key, Uni<String> value) {
      var cache = maps.get(name);
      if (cache != null) {
         cache.asMap().remove(key, value);
      }
   }

   public Uni<String> put(String name, String key, String value) {
      Uni<String> indexUni = searchBackend.put(name, key, value)
                  .map(___ -> value)
            // Make sure subsequent subscriptions don't update index again
                  .memoize().indefinitely();
      getOrCreateMap(name)
            .put(key, indexUni);
      // TODO: technically only want to do this on caller subscribing
      indexUni.subscribe()
                  .with(s -> replace(name, key, indexUni, value), t -> actualRemove(name, key, indexUni));
      return indexUni;
   }

   public Uni<String> get(String name, String key) {
      CacheAccessRecord<String> cacheAccessRecord = metrics.recordCacheAccess();
      try {
         Uni<String> uni = getOrCreateMap(name).get(key);
         cacheAccessRecord.localHit(uni instanceof UniItem<String>);
         return uni.onItemOrFailure().invoke(cacheAccessRecord);
      } catch (RuntimeException e) {
         cacheAccessRecord.recordThrowable(e);
         throw e;
      }
   }

   public Stream<String> getKeys(String name) {
      Cache<String, ?> cache = getOrCreateMap(name);
      return cache.asMap()
            .keySet()
            .stream();
   }

   public Uni<Boolean> remove(String name, String key) {
      Uni<String> indexUni = searchBackend.remove(name, key)
            .<String>map(___ -> null)
            // Make sure subsequent subscriptions don't update index again
            .memoize().indefinitely();
      // We put a null uni into the map if a value existed before. This way we cache the tombstone.
      Uni<String> prev = getOrCreateMap(name).asMap()
            .replace(key, indexUni);
      if (prev != null) {
         // TODO: technically only want to do this on caller subscribing
         indexUni.subscribe()
               .with(s -> replace(name, key, indexUni, null), t -> actualRemove(name, key, indexUni));
         // TODO: technically this says it removed something even if the Uni contained a null value prior
         return Uni.createFrom().item(Boolean.TRUE);
      }
      return Uni.createFrom().item(Boolean.FALSE);
   }
}
