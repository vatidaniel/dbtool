package io.github.vatidaniel.it;

import io.github.vatidaniel.metadata.core.ColumnMetadata;
import io.github.vatidaniel.metadata.core.TableMetadata;
import io.github.vatidaniel.metadata.postgres.PostgresDataType;
import io.github.vatidaniel.metadata.postgres.PostgresDdlExecutor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the generated PostgreSQL DDL against a real PostgreSQL instance. Requires Docker; enable with
 * {@code mvn -Pintegration-tests test}.
 *
 * @author tinhnv
 * @since Jun 13, 2026
 */
@Testcontainers
class PostgresDdlExecutorIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private Connection connect() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    @Test
    void createTableThenAddColumn_runAgainstRealPostgres() throws Exception {
        try (Connection conn = connect()) {
            TableMetadata create = TableMetadata.builder().name("person").columnsMetadata(List.of(
                ColumnMetadata.builder().name("id").dataType(PostgresDataType.INTEGER).primaryKey(true).nullable(false).identity(true).build(),
                ColumnMetadata.builder().name("name").dataType(PostgresDataType.VARCHAR).dataTypeExtension("100").build()
            )).build();
            new PostgresDdlExecutor(create, conn).createTable();

            assertTrue(columnExists(conn, "person", "id"), "id column should exist");
            assertTrue(columnExists(conn, "person", "name"), "name column should exist");

            TableMetadata withEmail = TableMetadata.builder().name("person").columnsMetadata(List.of(
                ColumnMetadata.builder().name("email").dataType(PostgresDataType.VARCHAR).dataTypeExtension("255").build()
            )).build();
            new PostgresDdlExecutor(withEmail, conn).addColumn("email");

            assertTrue(columnExists(conn, "person", "email"), "email column should exist after addColumn");
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws Exception {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, table, column)) {
            return rs.next();
        }
    }
}
