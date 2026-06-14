package io.github.vatisteve.it;

import io.github.vatisteve.metadata.core.ColumnMetadata;
import io.github.vatisteve.metadata.core.ConstraintType;
import io.github.vatisteve.metadata.core.TableMetadata;
import io.github.vatisteve.metadata.sqlserver.SqlServerDataType;
import io.github.vatisteve.metadata.sqlserver.SqlServerDdlExecutor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the generated SQL Server DDL against a real SQL Server instance, exercising the dialect-specific
 * statements ({@code ADD} without {@code COLUMN}, {@code ALTER COLUMN}, {@code sp_rename}). Requires
 * Docker; enable with {@code mvn -Pintegration-tests verify}.
 *
 * @author tinhnv
 * @since Jun 14, 2026
 */
@Testcontainers
class SqlServerDdlExecutorIT {

    @Container
    static final MSSQLServerContainer<?> MSSQL =
        new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private Connection connect() throws Exception {
        // disable TLS for the test container's self-signed certificate
        return DriverManager.getConnection(MSSQL.getJdbcUrl() + ";encrypt=false",
            MSSQL.getUsername(), MSSQL.getPassword());
    }

    @Test
    void createTableAlterColumnAndRename_runAgainstRealSqlServer() throws Exception {
        try (Connection conn = connect()) {
            TableMetadata create = TableMetadata.builder().name("person").columnsMetadata(List.of(
                ColumnMetadata.builder().name("id").dataType(SqlServerDataType.INT).primaryKey(true).nullable(false).identity(true).build(),
                ColumnMetadata.builder().name("name").dataType(SqlServerDataType.VARCHAR).dataTypeExtension("100").build()
            )).build();
            new SqlServerDdlExecutor(create, conn).createTable();

            assertTrue(columnExists(conn, "person", "id"), "id column should exist");
            assertTrue(columnExists(conn, "person", "name"), "name column should exist");

            // ADD without the COLUMN keyword
            TableMetadata withEmail = TableMetadata.builder().name("person").columnsMetadata(List.of(
                ColumnMetadata.builder().name("email").dataType(SqlServerDataType.VARCHAR).dataTypeExtension("255").build()
            )).build();
            new SqlServerDdlExecutor(withEmail, conn).addColumn("email");
            assertTrue(columnExists(conn, "person", "email"), "email column should exist after addColumn");

            // NOT NULL via ALTER COLUMN, then sp_rename for the column and the table
            SqlServerDdlExecutor exec = new SqlServerDdlExecutor(withEmail, conn);
            exec.addColumnConstraint(ConstraintType.NOT_NULL, "email");
            exec.renameColumn("email", "email_address");
            assertTrue(columnExists(conn, "person", "email_address"), "renamed column should exist");
            assertFalse(columnExists(conn, "person", "email"), "old column name should be gone");

            new SqlServerDdlExecutor(create, conn).renameTable("people");
            assertTrue(columnExists(conn, "people", "id"), "table should be reachable under the new name");
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws Exception {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, table, column)) {
            return rs.next();
        }
    }
}
