package io.gingersnapproject.calcite;

import java.sql.JDBCType;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.rel.type.RelProtoDataType;
import org.apache.calcite.schema.Function;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.SchemaVersion;
import org.apache.calcite.schema.Schemas;
import org.apache.calcite.schema.Table;
import org.apache.calcite.util.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.gingersnapproject.Caches;
import io.gingersnapproject.configuration.Configuration;
import io.gingersnapproject.database.DatabaseHandler;

@Singleton
public class CachesSchema implements Schema {
   @Inject
   private Caches caches;
   @Inject
   private DatabaseHandler dbHandler;
   @Inject
   private Configuration configuration;

   @Override
   public @Nullable Table getTable(String name) {
      Pair<String, JDBCType>[] metadata = dbHandler.metadata(name).await().indefinitely();
      return new CacheScannableTable(this, name, metadata);
   }

   @Override
   public Set<String> getTableNames() {
      return configuration.rules().keySet();
   }

   @Override
   public @Nullable RelProtoDataType getType(String name) {
      return null;
   }

   @Override
   public Set<String> getTypeNames() {
      return Collections.emptySet();
   }

   @Override
   public Collection<Function> getFunctions(String name) {
      return Collections.emptySet();
   }

   @Override
   public Set<String> getFunctionNames() {
      return Collections.emptySet();
   }

   @Override
   public @Nullable Schema getSubSchema(String name) {
      return null;
   }

   @Override
   public Set<String> getSubSchemaNames() {
      return Collections.emptySet();
   }

   @Override
   public Expression getExpression(@Nullable SchemaPlus parentSchema, String name) {
      return Schemas.subSchemaExpression(parentSchema, name, getClass());
   }

   @Override
   public boolean isMutable() {
      // New caches can be created at runtime
      return true;
   }

   @Override
   public Schema snapshot(SchemaVersion version) {
      // We don't support schema snapshots for now
      return this;
   }

   public Caches caches() {
      return caches;
   }
}
