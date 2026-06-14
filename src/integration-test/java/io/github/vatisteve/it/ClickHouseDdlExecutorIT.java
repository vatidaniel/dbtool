package io.github.vatisteve.it;

import io.github.vatisteve.metadata.clickhouse.ClickHouseColumnMetadata;
import io.github.vatisteve.metadata.clickhouse.ClickHouseDdlExecutor;
import io.github.vatisteve.metadata.core.ColumnMetadata;
import io.github.vatisteve.metadata.core.TableMetadata;
import org.junit.jupiter.api.Test;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the generated ClickHouse DDL against a real ClickHouse instance, covering its divergent shape
 * ({@code Nullable(...)} wrapping, {@code ENGINE}/{@code ORDER BY}, {@code ADD COLUMN ... FIRST}).
 * Requires Docker; enable with {@code mvn -Pintegration-tests verify}.
 *
 * @author tinhnv
 * @since Jun 14, 2026
 */
@Testcontainers
class ClickHouseDdlExecutorIT {

    @Container
    static final ClickHouseContainer CLICKHOUSE =
        new ClickHouseContainer("clickhouse/clickhouse-server:24.3-alpine");

    private Connection connect() throws Exception {
        return DriverManager.getConnection(CLICKHOUSE.getJdbcUrl(), CLICKHOUSE.getUsername(), CLICKHOUSE.getPassword());
    }

    @Test
    void createTableThenAddColumn_runAgainstRealClickHouse() throws Exception {
        try (Connection conn = connect()) {
            // id carries an ORDER BY index so the MergeTree engine has a sorting key
            TableMetadata create = TableMetadata.builder().name("person").columnsMetadata(List.of(
                ClickHouseColumnMetadata.builder().name("id").dataType("Int32").nullable(false).primaryKey(true).orderByIndex(1).build(),
                ColumnMetadata.builder().name("name").dataType("String").nullable(true).build()
            )).build();
            new ClickHouseDdlExecutor(create, conn).createTable();

            assertTrue(columnExists(conn, "person", "id"), "id column should exist");
            assertTrue(columnExists(conn, "person", "name"), "name column should exist");

            TableMetadata withEmail = TableMetadata.builder().name("person").columnsMetadata(List.of(
                ColumnMetadata.builder().name("email").dataType("String").nullable(true).build()
            )).build();
            new ClickHouseDdlExecutor(withEmail, conn).addColumn("email");

            assertTrue(columnExists(conn, "person", "email"), "email column should exist after addColumn");
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT count() FROM system.columns WHERE database = currentDatabase() AND table = ? AND name = ?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        }
    }
}
