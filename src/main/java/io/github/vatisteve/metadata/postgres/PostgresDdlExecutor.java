package io.github.vatisteve.metadata.postgres;

import io.github.vatisteve.dataaccess.PostgresDialect;
import io.github.vatisteve.metadata.core.ColumnMetadata;
import io.github.vatisteve.metadata.core.StandardSqlDdlExecutor;
import io.github.vatisteve.metadata.core.TableMetadata;

import java.sql.Connection;

/**
 * PostgreSQL DDL executor. Reuses the standard relational generation in {@link StandardSqlDdlExecutor}
 * via the {@link PostgresDialect}, overriding only the column-modify statement, whose shape differs from
 * MySQL/MariaDB.
 *
 * @author tinhnv
 * @since Jun 13, 2026
 */
public class PostgresDdlExecutor extends StandardSqlDdlExecutor {

    public PostgresDdlExecutor(TableMetadata tableMetadata, Connection connection) {
        super(tableMetadata, connection, new PostgresDialect());
    }

    @Override
    protected String buildUpdateColumnDefinitionSql(ColumnMetadata columnMetadata) {
        // PostgreSQL changes a column's type with ALTER COLUMN ... TYPE ... rather than MySQL's MODIFY.
        return ALTER_TABLE + getTableMetadata().getName() + " ALTER COLUMN "
            + getDialect().quoteIdentifier(columnMetadata.getName()) + " TYPE " + columnMetadata.getDataType();
    }

}
