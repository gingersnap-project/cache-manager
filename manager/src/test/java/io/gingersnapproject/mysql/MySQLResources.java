package io.gingersnapproject.mysql;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.MountableFile;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MySQLResources implements QuarkusTestResourceLifecycleManager {

    private static final String IMAGE = "mysql:8.0.31";
    private MySQLContainer<?> db;

    @Override
    public Map<String, String> start() {


        db = new MySQLContainer<>(IMAGE)
                .withUsername("gingersnap_user")
                .withPassword("password")
                .withExposedPorts(MySQLContainer.MYSQL_PORT)
                .withCopyFileToContainer(MountableFile.forClasspathResource("setup.sql"), "/docker-entrypoint-initdb.d/setup.sql");
        db.start();

        Map<String, String> properties = new HashMap<>(Map.of(
              "quarkus.datasource.db-kind", "MYSQL",
              "quarkus.datasource.username", db.getUsername(),
              "quarkus.datasource.password", db.getPassword(),
              "quarkus.datasource.reactive.url", String.format("mysql://%s:%d/debezium", db.getHost(), db.getMappedPort(MySQLContainer.MYSQL_PORT)),

              "gingersnap.rule.us-east.key-type", "PLAIN",
              "gingersnap.rule.us-east.plain-separator", ":",
              "gingersnap.rule.us-east.select-statement", "select fullname, email from customer where id = ?",
              "gingersnap.rule.us-east.connector.schema", "debezium",
              "gingersnap.rule.us-east.connector.table", "customer"));

        for (int i = 1; i < 4; ++i) {
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