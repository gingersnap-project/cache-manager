package io.gingersnapproject;

import java.util.List;
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
import io.smallrye.mutiny.Uni;

@Singleton
public class Caches {
   @Inject
   CacheManagerMetrics metrics;
   private final ConcurrentMap<String, LoadingCache<String, Uni<String>>> maps = new ConcurrentHashMap<>();
   private final ConcurrentMap<String, Cache<String, List<byte[]>>> multiMaps = new ConcurrentHashMap<>();

   @Inject
   DatabaseHandler databaseHandler;
   @Inject
   Configuration configuration;

   public ConcurrentMap<String, Cache<String, List<byte[]>>> getMultiMaps() {
      return multiMaps;
   }

   private LoadingCache<String, Uni<String>> getOrCreateMap(String name) {
      if (!configuration.rules().containsKey(name)) {
         // If no rule defined, don't apply database loading (internal caches)
         return maps.computeIfAbsent(name,  ___ ->  Caffeine.newBuilder().build(k -> null));
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

   public void put(String name, String key, String value) {
      getOrCreateMap(name)
            .put(key, UniItem.fromItem(value));
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

   public boolean remove(String name, String key) {
      // TODO: technically this says it removed something even if the Uni contained a null value prior
      // We put a null uni into the map if a value existed before. This way we cache the tombstone.
      return getOrCreateMap(name).asMap()
            .replace(key, Uni.createFrom().nullItem()) != null;
   }
}
