package io.gingersnapproject.database;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.infinispan.commons.dataconversion.internal.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.gingersnapproject.configuration.Configuration;
import io.gingersnapproject.configuration.Rule;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;
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

   @Inject
   Pool pool;

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
