package io.github.vatisteve.it;

import io.github.vatisteve.metadata.core.ColumnMetadata;
import io.github.vatisteve.metadata.core.TableMetadata;
import io.github.vatisteve.metadata.mariadb.MariadbDdlExecutor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the generated MySQL/MariaDB DDL against a real MySQL instance. Requires Docker; enable with
 * {@code mvn -Pintegration-tests test}.
 *
 * @author tinhnv
 * @since Jun 13, 2026
 */
@Testcontainers
class MariadbDdlExecutorIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    private Connection connect() throws Exception {
        return DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    @Test
    void createTableThenAddColumn_runAgainstRealMysql() throws Exception {
        try (Connection conn = connect()) {
            TableMetadata create = TableMetadata.builder().name("person").columnsMetadata(List.of(
                ColumnMetadata.builder().name("id").dataType("INT").primaryKey(true).nullable(false).identity(true).build(),
                ColumnMetadata.builder().name("name").dataType("VARCHAR").dataTypeExtension("100").build()
            )).build();
            new MariadbDdlExecutor(create, conn).createTable();

            assertTrue(columnExists(conn, "person", "id"), "id column should exist");
            assertTrue(columnExists(conn, "person", "name"), "name column should exist");

            TableMetadata withEmail = TableMetadata.builder().name("person").columnsMetadata(List.of(
                ColumnMetadata.builder().name("email").dataType("VARCHAR").dataTypeExtension("255").build()
            )).build();
            new MariadbDdlExecutor(withEmail, conn).addColumn("email");

            assertTrue(columnExists(conn, "person", "email"), "email column should exist after addColumn");
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws Exception {
        try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }
}
