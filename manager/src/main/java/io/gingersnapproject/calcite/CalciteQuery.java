package io.gingersnapproject.calcite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.Driver;
import org.apache.calcite.schema.SchemaPlus;

@Singleton
public class CalciteQuery {

   @Inject
   private CachesSchema schema;

   private CalciteConnection connection;

   public CalciteQuery() {
      try {
         connection = DriverManager.getConnection(Driver.CONNECT_STRING_PREFIX, new Properties()).unwrap(CalciteConnection.class);
         SchemaPlus rootSchema = connection.getRootSchema();
         rootSchema.add("gingersnap", schema);
      } catch (SQLException e) {
         // log it
      }
   }

   public Connection getConnection() {
      return connection;
   }

}
