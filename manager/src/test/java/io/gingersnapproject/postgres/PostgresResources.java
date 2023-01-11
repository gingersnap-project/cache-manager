package io.gingersnapproject.postgres;

import io.gingersnapproject.database.DatabaseResourcesLifecyleManager;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.util.Map;

public class PostgresResources implements DatabaseResourcesLifecyleManager.Database {

    private static final String IMAGE = "postgres:latest";

    private static final String HOST_TMP = Path.of(System.getProperty("java.io.tmpdir"), "postgres_cache_manager").toString();
    private static final String CONTAINER_DATA_DIR = "/var/lib/postgresql/data";

    private PostgreSQLContainer<?> db;

    @Override
    public void start() {
        db = new PostgreSQLContainer<>(IMAGE)
                .withDatabaseName("debeziumdb")
                .withUsername("gingersnap_user")
                .withPassword("password")
                .withExposedPorts(PostgreSQLContainer.POSTGRESQL_PORT)
                .withFileSystemBind(HOST_TMP, CONTAINER_DATA_DIR, BindMode.READ_WRITE)
                .withCopyFileToContainer(MountableFile.forClasspathResource("postgres/setup.sql"), "/docker-entrypoint-initdb.d/setup.sql");
        db.start();
    }

    @Override
    public void initProperties(Map<String, String> props) {
        props.put("quarkus.datasource.username", db.getUsername());
        props.put("quarkus.datasource.password", db.getPassword());
        props.put("quarkus.datasource.reactive.url", String.format("postgresql://%s:%d/debeziumdb", db.getHost(), db.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)));
    }


    @Override
    public void stop() {
        if (db != null)
            db.stop();
    }
}
