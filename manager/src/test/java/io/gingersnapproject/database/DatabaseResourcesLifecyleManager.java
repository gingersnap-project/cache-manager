package io.gingersnapproject.database;

import io.gingersnapproject.mysql.MySQLResources;
import io.gingersnapproject.postgres.PostgresResources;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

import java.util.HashMap;
import java.util.Map;

public class DatabaseResourcesLifecyleManager implements QuarkusTestResourceLifecycleManager {

    public static final String RULE_NAME = "us-east";

    private Database db;

    @Override
    public void setContext(Context context) {
        var dbKind = System.getProperty("quarkus.datasource.db-kind");
        switch (dbKind) {
            case "postgresql" -> db = new PostgresResources();
            default -> db = new MySQLResources();
        }
    }

    @Override
    public Map<String, String> start() {
        db.start();

        Map<String, String> properties = new HashMap<>( Map.of(
                String.format("gingersnap.rule.%s.key-type", RULE_NAME), "PLAIN",
                String.format("gingersnap.rule.%s.plain-separator", RULE_NAME), ":",
                String.format("gingersnap.rule.%s.select-statement", RULE_NAME), "select fullname, email from customer where id = ?",
                String.format("gingersnap.rule.%s.connector.schema", RULE_NAME), "debezium",
                String.format("gingersnap.rule.%s.connector.table", RULE_NAME), "customer"));

        for (int i = 1; i < 5; ++i) {
            properties.putAll(Map.of(
                    "gingersnap.rule.developers-" + i + ".key-type", "PLAIN",
                    "gingersnap.rule.developers-" + i + ".plain-separator", ":",
                    "gingersnap.rule.developers-" + i + ".select-statement", "select fullname, email from customer where id = ?",
                    "gingersnap.rule.developers-" + i + ".query-enabled", "true",
                    "gingersnap.rule.developers-" + i + ".connector.schema", "debezium",
                    "gingersnap.rule.developers-" + i + ".connector.table", "developers-" + i));
        }
        db.initProperties(properties);
        return properties;
    }

    @Override
    public void stop() {
        db.stop();
    }

    public interface Database {

        void initProperties(Map<String, String> props);

        void start();

        void stop();
    }
}
