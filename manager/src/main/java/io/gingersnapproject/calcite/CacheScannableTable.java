package io.gingersnapproject.calcite;

import java.sql.JDBCType;
import java.util.Map;

import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.util.Pair;

public class CacheScannableTable extends CacheTable implements ScannableTable {
   public CacheScannableTable(CachesSchema schema, String name, Pair<String, JDBCType>[] metadata) {
      super(schema, name, metadata);
   }

   @Override
   public Enumerable<Object[]> scan(DataContext root) {
      return new AbstractEnumerable<>() {
         @Override
         public Enumerator<Object[]> enumerator() {
            return new CacheEnumerator(CacheScannableTable.this, schema.caches().cacheIterator(name));
         }
      };
   }
}
