package io.gingersnapproject.mysql;

import io.gingersnapproject.database.DatabaseResourcesLifecyleManager;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.util.Map;

public class MySQLResources implements DatabaseResourcesLifecyleManager.Database {

    private static final String IMAGE = "mysql:8.0.31";

    private static final String HOST_TMP = Path.of(System.getProperty("java.io.tmpdir"), "mysql_cache_manager").toString();
    private static final String CONTAINER_DATA_DIR = "/var/lib/mysql";

    private MySQLContainer<?> db;

    @Override
    public void start() {
        db = new MySQLContainer<>(IMAGE)
                .withUsername("gingersnap_user")
                .withPassword("password")
                .withExposedPorts(MySQLContainer.MYSQL_PORT)
                .withFileSystemBind(HOST_TMP, CONTAINER_DATA_DIR, BindMode.READ_WRITE)
                .withCopyFileToContainer(MountableFile.forClasspathResource("mysql/setup.sql"), "/docker-entrypoint-initdb.d/setup.sql");
        db.start();
    }

   @Override
   public void initProperties(Map<String, String> props) {
      props.put("quarkus.datasource.username", db.getUsername());
      props.put("quarkus.datasource.password", db.getPassword());
      props.put("quarkus.datasource.reactive.url", String.format("mysql://%s:%d/debezium", db.getHost(), db.getMappedPort(MySQLContainer.MYSQL_PORT)));
   }


   @Override
    public void stop() {
        if (db != null)
            db.stop();
    }
}
