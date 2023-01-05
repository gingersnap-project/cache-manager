package io.gingersnapproject.database;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.infinispan.commons.dataconversion.internal.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.gingersnapproject.configuration.Configuration;
import io.gingersnapproject.configuration.Rule;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.db2client.DB2Pool;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.mutiny.sqlclient.Tuple;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.oracleclient.OraclePool;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;

@Singleton
public class DatabaseHandler {
   private static final Logger log = LoggerFactory.getLogger(DatabaseHandler.class);
   @Inject
   Configuration configuration;

   Pool pool;

   void checkPoolConfigured(Config config, String dbName, Class<? extends Pool> implClass) {
      if (config.getOptionalValue("quarkus.datasource." + dbName + ".reactive.url", String.class).isPresent()) {
         if (pool != null) {
            throw new IllegalStateException("Multiple reactive urls defined!");
         }
         ArcContainer arcContainer = Arc.container();
         InstanceHandle<? extends Pool> instance = arcContainer.instance(implClass, new ReactiveDataSource.ReactiveDataSourceLiteral(dbName));
         pool = instance.get();
      }
   }

   void startup(@Observes StartupEvent event, Config config) {
      checkPoolConfigured(config, "mysql", MySQLPool.class);
      checkPoolConfigured(config, "mariadb", MySQLPool.class);
      checkPoolConfigured(config, "db2", DB2Pool.class);
      checkPoolConfigured(config, "mssql", MSSQLPool.class);
      checkPoolConfigured(config, "oracle", OraclePool.class);
      checkPoolConfigured(config, "postgresql", PgPool.class);
      if (pool == null) {
         throw new IllegalStateException("There was no reactive url defined in the configuration!");
      }
   }

   public Uni<String> select(String rule, String key) {
      Rule ruleConfig = configuration.rules().get(rule);
      if (ruleConfig == null) {
         throw new IllegalArgumentException("No rule found for " + rule);
      }

      // Have to do this until bug is fixed allowing injection of reactive Pool
      PreparedQuery<RowSet<Row>> preparedQuery = pool.preparedQuery(ruleConfig.selectStatement());
      var quarkusPreparedQuery = io.vertx.mutiny.sqlclient.PreparedQuery.<RowSet<Row>>newInstance(preparedQuery);
      
      String[] arguments = ruleConfig.keyType().toArguments(key, ruleConfig.plainSeparator());
//      return pool.preparedQuery(ruleConfig.selectStatement())
      return quarkusPreparedQuery
            .execute(Tuple.from(arguments))
            .onFailure().invoke(t -> log.error("Exception encountered!", t))
            .map(rs -> {
               if (rs.size() > 1) {
                  throw new IllegalArgumentException("Result set for " + ruleConfig.selectStatement() + " for key: " + key + " returned " + rs.size() + " rows, it should only return 1");
               }
               int columns = rs.columnsNames().size();
               RowIterator<Row> rowIterator = rs.iterator();
               if (!rowIterator.hasNext()) {
                  return null;
               }
               Row row = rowIterator.next();
               Json jsonObject = Json.object();
               for (int i = 0; i < columns; ++i) {
                  jsonObject.set(row.getColumnName(i), row.getValue(i));
               }
               return jsonObject.toString();
            });
   }
}
