package io.gingersnapproject.calcite;

import java.util.Iterator;
import java.util.Map;

import org.apache.calcite.linq4j.Enumerator;
import org.infinispan.commons.dataconversion.internal.Json;

import io.smallrye.mutiny.Uni;

public class CacheEnumerator implements Enumerator<Object[]> {
   private final Iterator<? extends Map.Entry<String, Uni<String>>> iterator;
   private final CacheScannableTable table;
   private final int size;
   private Map.Entry<String, Uni<String>> current;

   public CacheEnumerator(CacheScannableTable table, Iterator<? extends Map.Entry<String, Uni<String>>> iterator) {
      this.table = table;
      this.iterator = iterator;
      this.size = 1 + table.metadata.length;
   }

   @Override
   public Object[] current() {
      Object[] row = new Object[size];
      row[0] = current.getKey();
      Json json = Json.read(current.getValue().await().indefinitely());
      for (int i = 0; i < table.metadata.length; i++) {
         Json jsonField = json.at(table.metadata[i].getKey());
         // TODO: complete
         row[i + 1] = switch (table.metadata[i].getValue()) {
            case VARCHAR -> jsonField.asString();
            case INTEGER -> jsonField.asInteger();
            default -> null;
         };

      }
      return row;
   }

   @Override
   public boolean moveNext() {
      if (iterator.hasNext()) {
         current = iterator.next();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void reset() {

   }

   @Override
   public void close() {
   }
}
