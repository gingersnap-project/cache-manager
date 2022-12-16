package io.gingersnapproject.mysql;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MySQLResources implements QuarkusTestResourceLifecycleManager {

   public static final String RULE = "us-east";
   private static final String IMAGE = "mysql:8.0.31";

   private static final String HOST_TMP = Path.of(System.getProperty("java.io.tmpdir"), "mysql_cache_manager").toString();
   private static final String CONTAINER_DATA_DIR = "/var/lib/mysql";

   private MySQLContainer<?> db;

   @Override
   public Map<String, String> start() {
      db = new MySQLContainer<>(IMAGE)
            .withUsername("gingersnap_user")
            .withPassword("password")
            .withExposedPorts(MySQLContainer.MYSQL_PORT)
            .withFileSystemBind(HOST_TMP, CONTAINER_DATA_DIR, BindMode.READ_WRITE)
            .withCopyFileToContainer(MountableFile.forClasspathResource("setup.sql"), "/docker-entrypoint-initdb.d/setup.sql");
      db.start();

      Map<String, String> properties = new HashMap<>( Map.of(
            "quarkus.datasource.db-kind", "MYSQL",
            "quarkus.datasource.username", db.getUsername(),
            "quarkus.datasource.password", db.getPassword(),
            "quarkus.datasource.reactive.url", String.format("mysql://%s:%d/debezium", db.getHost(), db.getMappedPort(MySQLContainer.MYSQL_PORT)),
            String.format("gingersnap.rule.%s.key-type", RULE), "PLAIN",
            String.format("gingersnap.rule.%s.plain-separator", RULE), ":",
            String.format("gingersnap.rule.%s.select-statement", RULE), "select fullname, email from customer where id = ?",
            String.format("gingersnap.rule.%s.connector.schema", RULE), "debezium",
            String.format("gingersnap.rule.%s.connector.table", RULE), "customer"));

      for (int i = 1; i < 5; ++i) {
          properties.putAll(Map.of(
            "gingersnap.rule.developers-" + i + ".key-type", "PLAIN",
            "gingersnap.rule.developers-" + i + ".plain-separator", ":",
            "gingersnap.rule.developers-" + i + ".select-statement", "select fullname, email from customer where id = ?",
            "gingersnap.rule.developers-" + i + ".query-enabled", "true",
            "gingersnap.rule.developers-" + i + ".connector.schema", "debezium",
            "gingersnap.rule.developers-" + i + ".connector.table", "developers-" + i));
      }

      return properties;
   }

   @Override
   public void stop() {
      if (db != null)
         db.stop();
   }
}