package io.gingersnapproject.hotrod;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.commons.util.Util;
import org.infinispan.server.hotrod.HotRodOperation;
import org.infinispan.server.hotrod.OperationStatus;
import org.infinispan.server.hotrod.ProtocolFlag;
import org.infinispan.server.hotrod.logging.Log;
import org.infinispan.util.concurrent.TimeoutException;

import io.gingersnapproject.Caches;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.smallrye.mutiny.Uni;

public class CommandProcessor {
   private static final Log log = LogFactory.getLog(CommandProcessor.class, Log.class);
   private static final String INTERNAL_MAP_PREFIX = "___";
   private static final String OFFSET_MAP_NAME = INTERNAL_MAP_PREFIX + "debezium-offset";
   private static final String SCHEMA_MULTI_MAP_NAME = INTERNAL_MAP_PREFIX + "debezium-schema";

   private final Channel channel;
   private final Caches maps;

   private final ConcurrentMap<WrappedByteArray, WrappedByteArray> debeziumOffsetMap = new ConcurrentHashMap<>();
   private final ConcurrentMap<WrappedByteArray, List<byte[]>> debeziumSchemaMultiMap = new ConcurrentHashMap<>();

   public CommandProcessor(Channel channel, Caches maps) {
      this.channel = channel;
      this.maps = maps;
   }

   private static String toString(byte[] bytes) {
      return new String(bytes, StandardCharsets.UTF_8);
   }

   private static byte[] toByteArray(String str) {
      return str == null ? null : str.getBytes(StandardCharsets.UTF_8);
   }

   public void put(GingersnapHeader header, byte[] key, byte[] value) {
      if (header.hasFlag(ProtocolFlag.ForceReturnPreviousValue)) {
         writeException(header, new UnsupportedOperationException("previous value is not supported!"));
         return;
      }

      try {
         String cacheName = header.getCacheName();
         if (cacheName.startsWith("___")) {
            if (!OFFSET_MAP_NAME.equals(cacheName)) {
               writeException(header, new IllegalArgumentException("Cache of name " + cacheName + " not supported in Gingersnap."));
               return;
            }
            debeziumOffsetMap.put(new WrappedByteArray(key), new WrappedByteArray(value));
            writeSuccess(header);
            return;
         }

         Uni<String> putUni = maps.put(header.getCacheName(), toString(key), toString(value));
         putUni.subscribe().with(___ -> writeSuccess(header), t -> writeException(header, t));
      } catch (Throwable t) {
         writeException(header, t);
      }
   }

   private void handleGetResponse(GingersnapHeader header, byte[] value) {
      if (value == null) {
         writeNotExist(header);
      } else {
         writeResponse(header.encoder().valueResponse(header, null, channel, OperationStatus.Success, value));
      }
   }

   public void get(GingersnapHeader header, byte[] key) {
      try {
         String cacheName = header.getCacheName();
         if (cacheName.startsWith(INTERNAL_MAP_PREFIX)) {
            if (!OFFSET_MAP_NAME.equals(cacheName)) {
               writeException(header, new IllegalArgumentException("Cache of name " + cacheName + " not supported in Gingersnap."));
               return;
            }
            WrappedByteArray value = debeziumOffsetMap.get(new WrappedByteArray(key));
            handleGetResponse(header, value.getBytes());
            return;
         }

         // This can never be null
         Uni<String> uniValue = maps.get(header.getCacheName(), toString(key));
         uniValue.subscribe().with(s -> handleGetResponse(header, toByteArray(s)),
               t -> writeException(header, t));
      } catch (Throwable t) {
         writeException(header, t);
      }
   }

   public void remove(GingersnapHeader header, byte[] key) {
      if (header.hasFlag(ProtocolFlag.ForceReturnPreviousValue)) {
         writeException(header, new UnsupportedOperationException("previous value is not supported!"));
         return;
      }

      try {
         String cacheName = header.getCacheName();
         if (cacheName.startsWith(INTERNAL_MAP_PREFIX)) {
            if (!OFFSET_MAP_NAME.equals(cacheName)) {
               writeException(header, new IllegalArgumentException("Cache of name " + cacheName + " not supported in Gingersnap."));
               return;
            }
            if (debeziumOffsetMap.remove(new WrappedByteArray(key)) != null) {
               writeSuccess(header);
            } else {
               writeNotExist(header);
            }
            return;
         }

         Uni<Boolean> removeUni = maps.remove(header.getCacheName(), toString(key));
         removeUni.subscribe().with(removed -> {
            if (removed) {
               writeSuccess(header);
            } else {
               writeNotExist(header);
            }
         }, t -> writeException(header, t));
      } catch (Throwable t) {
         writeException(header, t);
      }
   }

   public void ping(GingersnapHeader header) {
      this.writeResponse(header.encoder().pingResponse(header, null, this.channel, OperationStatus.Success));
   }

   public void putAll(GingersnapHeader header, Map<byte[], byte[]> entryMap) {

      try {
         String cacheName = header.getCacheName();
         if (cacheName.startsWith(INTERNAL_MAP_PREFIX)) {
            if (!OFFSET_MAP_NAME.equals(cacheName)) {
               writeException(header, new IllegalArgumentException("Cache of name " + cacheName + " not supported in Gingersnap."));
               return;
            }
            for (Map.Entry<byte[], byte[]> entry : entryMap.entrySet()) {
               debeziumOffsetMap.put(new WrappedByteArray(entry.getKey()), new WrappedByteArray(entry.getValue()));
            }
            writeSuccess(header);
            return;
         }
         for (Map.Entry<byte[], byte[]> entry : entryMap.entrySet()) {
            maps.put(cacheName, toString(entry.getKey()), toString(entry.getValue()));
         }
         writeSuccess(header);
      } catch (Throwable t) {
         writeException(header, t);
      }
   }

   public void getAll(GingersnapHeader header, Set<byte[]> keys) {
      try {
         String cacheName = header.getCacheName();
         if (cacheName.startsWith(INTERNAL_MAP_PREFIX)) {
            if (!OFFSET_MAP_NAME.equals(cacheName)) {
               writeException(header, new IllegalArgumentException("Cache of name " + cacheName + " not supported in Gingersnap."));
               return;
            }

            Map<byte[], byte[]> response = new HashMap<>();
            for (byte[] key : keys) {
               WrappedByteArray value = debeziumOffsetMap.get(new WrappedByteArray(key));
               if (value != null) {
                  response.put(key, value.getBytes());
               }
            }
            writeResponse(header.encoder().getAllResponse(header, null, channel, response));
            return;
         }

         Set<String> keysTransformed = new HashSet<>();
         for (byte[] key : keys) {
            keysTransformed.add(toString(key));
         }

         maps.getAll(cacheName, keysTransformed).subscribe()
                 .with(s -> {
                    Map<byte[], byte[]> transformed = new HashMap<>();
                    for (Map.Entry<String, String> entry : s.entrySet()) {
                       transformed.put(toByteArray(entry.getKey()), toByteArray(entry.getValue()));
                    }
                    writeResponse(header.encoder().getAllResponse(header, null, channel, transformed));
                 }, t -> writeException(header, t));
      } catch (Throwable t) {
         writeException(header, t);
      }
   }

   public void stats(GingersnapHeader header) {
      writeException(header, new UnsupportedOperationException("Stats not supported yet!"));
   }

   public void query(GingersnapHeader header, byte[] query) {
      writeException(header, new UnsupportedOperationException("Query not supported yet!"));
   }

   public void exec(GingersnapHeader header, String task, Map<String, byte[]> taskParams) {
      // TODO: do we support tasks? If anything this is just so adding a new cache doesn't error
      writeResponse(header.encoder().valueResponse(header, null, channel, OperationStatus.Success, Util.EMPTY_BYTE_ARRAY));
   }

   public void iterationStart(GingersnapHeader header, byte[] segmentMask, String filterConverterFactory, List<byte[]> filterConverterParams, int batchSize, boolean includeMetadat) {
      writeException(header, new UnsupportedOperationException("Iteration not supported yet!"));
   }

   public void iterationNext(GingersnapHeader header, String iterationId) {
      writeException(header, new UnsupportedOperationException("Iteration not supported yet!"));
   }

   public void iterationEnd(GingersnapHeader header, String iterationId) {
      writeException(header, new UnsupportedOperationException("Iteration not supported yet!"));
   }

   void writeResponse(ByteBuf buf) {
      channel.writeAndFlush(buf);
   }

   void writeSuccess(GingersnapHeader header) {
      writeResponse(header.encoder().emptyResponse(header, null, channel, OperationStatus.Success));
   }

   void writeNotExist(GingersnapHeader header) {
      writeResponse(header.encoder().notExistResponse(header, null, channel));
   }

   public void writeException(GingersnapHeader header, Throwable cause) {
      if (cause instanceof CompletionException && cause.getCause() != null) {
         cause = cause.getCause();
      }
      String msg = cause.toString();
      OperationStatus status;
      if (cause instanceof InvalidMagicIdException) {
         log.exceptionReported(cause);
         status = OperationStatus.InvalidMagicOrMsgId;
      } else if (cause instanceof HotRodUnknownOperationException hruoe) {
         log.exceptionReported(cause);
         header = hruoe.toHeader();
         status = OperationStatus.UnknownOperation;
      } else if (cause instanceof UnknownVersionException uve) {
         log.exceptionReported(cause);
         header = uve.toHeader();
         status = OperationStatus.UnknownVersion;
      } else if (cause instanceof RequestParsingException rpe) {
         if (cause instanceof CacheNotFoundException)
            log.debug(cause.getMessage());
         else
            log.exceptionReported(cause);

         msg = cause.getCause() == null ? cause.toString() : format("%s: %s", cause.getMessage(), cause.getCause().toString());
         header = rpe.toHeader();
         status = OperationStatus.ParseError;
      } else if (cause instanceof IOException) {
         status = OperationStatus.ParseError;
      } else if (cause instanceof TimeoutException) {
         status = OperationStatus.OperationTimedOut;
      } else if (cause instanceof IllegalStateException) {
         // Some internal server code could throw this, so make sure it's logged
         log.exceptionReported(cause);
         if (header != null) {
            status = header.encoder().errorStatus(cause);
            msg = createErrorMsg(cause);
         } else {
            status = OperationStatus.ServerError;
         }
      } else if (header != null) {
         log.exceptionReported(cause);
         status = header.encoder().errorStatus(cause);
         msg = createErrorMsg(cause);
      } else {
         log.exceptionReported(cause);
         status = OperationStatus.ServerError;
      }
      if (header == null) {
         header = new GingersnapHeader(HotRodOperation.ERROR, (byte) 0, 0, "", 0, (short) 1, 0,
               MediaType.MATCH_ALL, MediaType.MATCH_ALL, null);
      } else {
         header = new GingersnapHeader(HotRodOperation.ERROR, header.getVersion(), header.getMessageId(), header.getCacheName(),
               header.getFlag(), header.getClientIntel(), header.getTopologyId(), header.getKeyMediaType(),
               header.getValueMediaType(), null);
      }
      ByteBuf buf = header.encoder().errorResponse(header, null, channel, msg, status);
      channel.writeAndFlush(buf);
   }

   private String createErrorMsg(Throwable t) {
      Set<Throwable> causes = new LinkedHashSet<>();
      Throwable initial = t;
      while (initial != null && !causes.contains(initial)) {
         causes.add(initial);
         initial = initial.getCause();
      }
      return causes.stream().map(Object::toString).collect(Collectors.joining("\n"));
   }

   public void getMultiMap(GingersnapHeader header, byte[] key) {
      if (!SCHEMA_MULTI_MAP_NAME.equals(header.getCacheName())) {
         writeException(header, new IllegalArgumentException("Multimap of name " + header.getCacheName() + " not supported in Gingersnap."));
      }
      try {
         List<byte[]> list = debeziumSchemaMultiMap.get(new WrappedByteArray(key));
         if (list == null) {
            writeNotExist(header);
         } else {
            writeResponse(header.encoder().multimapCollectionResponse(header, null, channel, OperationStatus.Success, list));
         }
      } catch (Throwable t) {
         writeException(header, t);
      }
   }

   public void putMultiMap(GingersnapHeader header, byte[] key, byte[] value) {
      if (!SCHEMA_MULTI_MAP_NAME.equals(header.getCacheName())) {
         writeException(header, new IllegalArgumentException("Multimap of name " + header.getCacheName() + " not supported in Gingersnap."));
      }
      try {
         debeziumSchemaMultiMap.compute(new WrappedByteArray(key), (k, v) -> {
            if (v == null) {
               var list = new ArrayList<byte[]>();
               list.add(value);
               return list;
            }
            v.add(value);
            return v;
         });
         writeSuccess(header);
      } catch (Throwable t) {
         writeException(header, t);
      }
   }
}
