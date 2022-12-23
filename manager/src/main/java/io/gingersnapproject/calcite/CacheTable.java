package io.gingersnapproject.calcite;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.Pair;

abstract class CacheTable extends AbstractTable {
   protected final CachesSchema schema;
   protected final String name;
   protected final Pair<String, JDBCType>[] metadata;
   protected RelDataType rowType;

   CacheTable(CachesSchema schema, String name, Pair<String, JDBCType>[] metadata) {
      this.schema = schema;
      this.name = name;
      this.metadata = metadata;
   }

   @Override
   public RelDataType getRowType(RelDataTypeFactory typeFactory) {
      if (rowType == null) {
         final List<RelDataType> types = new ArrayList<>();
         final List<String> names = new ArrayList<>();
         for (Pair<String, JDBCType> field : metadata) {
            names.add(field.getKey());
            SqlTypeName typeName = SqlTypeName.getNameForJdbcType(field.getValue().getVendorTypeNumber());
            types.add(typeFactory.createSqlType(typeName));
         }
         rowType = typeFactory.createStructType(types, names);
      }
      return rowType;
   }
}
