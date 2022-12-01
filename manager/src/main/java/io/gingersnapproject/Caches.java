package io.gingersnapproject;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import javax.inject.Singleton;

import org.infinispan.commons.marshall.WrappedByteArray;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Singleton
public class Caches {
   private final ConcurrentMap<String, Cache<String, WrappedByteArray>> maps = new ConcurrentHashMap<>();
   private final ConcurrentMap<String, Cache<String, List<byte[]>>> multiMaps = new ConcurrentHashMap<>();

   private Cache<String, WrappedByteArray> getOrCreateMap(String name) {
      // TODO: eventually need to apply eviction to this. Can use a weigher to count byte[] size per key/value
      return maps.computeIfAbsent(name, ___ -> Caffeine.newBuilder().build());
   }

   public byte[] get(String name, String key) {
      WrappedByteArray value = getOrCreateMap(name).getIfPresent(key);
      return value == null ? null : value.getBytes();
   }

   public Stream<String> getKeys(String name) {
      Cache<String, WrappedByteArray> cache = getOrCreateMap(name);
      return cache.asMap()
            .keySet()
            .stream();
   }

   public ConcurrentMap<String, Cache<String, WrappedByteArray>> getMaps() {
      return maps;
   }

   public ConcurrentMap<String, Cache<String, List<byte[]>>> getMultiMaps() {
      return multiMaps;
   }
}
