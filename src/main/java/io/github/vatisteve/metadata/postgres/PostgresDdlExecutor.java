package io.github.vatisteve.metadata.postgres;

import io.github.vatisteve.dataaccess.PostgresDialect;
import io.github.vatisteve.metadata.core.ColumnMetadata;
import io.github.vatisteve.metadata.core.ConstraintType;
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
        super(tableMetadata, connection, PostgresDialect.INSTANCE);
    }

    @Override
    protected String buildUpdateColumnDefinitionSql(ColumnMetadata columnMetadata) {
        // PostgreSQL changes a column's type with ALTER COLUMN ... TYPE ... rather than MySQL's MODIFY.
        return ALTER_TABLE + quotedTableName() + " ALTER COLUMN "
            + getDialect().quoteIdentifier(columnMetadata.getName()) + " TYPE " + columnMetadata.getDataType();
    }

    @Override
    protected String buildAddConstraintSql(ConstraintType constraintType, ColumnMetadata column) {
        if (constraintType == ConstraintType.NOT_NULL) {
            // PostgreSQL toggles NOT NULL via ALTER COLUMN, not MySQL's MODIFY <full column def>.
            return ALTER_TABLE + quotedTableName() + " ALTER COLUMN "
                + getDialect().quoteIdentifier(column.getName()) + " SET NOT NULL";
        }
        return super.buildAddConstraintSql(constraintType, column);
    }

    @Override
    protected String buildDropConstraintSql(ConstraintType constraintType, String name) {
        String table = quotedTableName();
        if (constraintType == ConstraintType.NOT_NULL) {
            // 'name' is the column name; PostgreSQL drops NOT NULL via ALTER COLUMN.
            return ALTER_TABLE + table + " ALTER COLUMN " + getDialect().quoteIdentifier(name) + " DROP NOT NULL";
        }
        // PostgreSQL drops every named constraint (PK/UNIQUE/CHECK/FK) the same way.
        return ALTER_TABLE + table + " DROP CONSTRAINT " + name;
    }

}
