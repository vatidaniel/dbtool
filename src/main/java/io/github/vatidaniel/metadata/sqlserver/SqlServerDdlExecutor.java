package io.github.vatidaniel.metadata.sqlserver;

import io.github.vatidaniel.dataaccess.SqlServerDialect;
import io.github.vatidaniel.metadata.core.ColumnMetadata;
import io.github.vatidaniel.metadata.core.ConstraintType;
import io.github.vatidaniel.metadata.core.StandardSqlDdlExecutor;
import io.github.vatidaniel.metadata.core.TableMetadata;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Microsoft SQL Server DDL executor. Reuses {@link StandardSqlDdlExecutor} via {@link SqlServerDialect}
 * and overrides the statements whose shape diverges from MySQL/MariaDB: {@code ADD} (no {@code COLUMN}
 * keyword), {@code ALTER COLUMN} for type/nullability changes, {@code sp_rename} for renames and
 * {@code DROP CONSTRAINT} for named constraints.
 *
 * @author tinhnv
 * @since Jun 13, 2026
 */
public class SqlServerDdlExecutor extends StandardSqlDdlExecutor {

    public SqlServerDdlExecutor(TableMetadata tableMetadata, Connection connection) {
        super(tableMetadata, connection, SqlServerDialect.INSTANCE);
    }

    @Override
    public void addColumn(String columnName) throws SQLException {
        // SQL Server uses ALTER TABLE ... ADD <col def>, without the COLUMN keyword.
        StringBuilder sql = new StringBuilder(ALTER_TABLE).append(quotedTableName()).append(" ADD ");
        appendColumnSql(sql, getColumnMetadata(columnName));
        executeSql(sql.toString());
    }

    @Override
    public void renameTable(String newName) throws SQLException {
        // sp_rename takes string literals, not bracketed identifiers; quote/escape them as literals.
        executeSql("EXEC sp_rename " + singleQuoteWrap(getTableMetadata().getName()) + ", " + singleQuoteWrap(newName));
        getTableMetadata().setName(newName);
    }

    @Override
    public void renameColumn(String oldName, String newName) throws SQLException {
        executeSql("EXEC sp_rename " + singleQuoteWrap(getTableMetadata().getName() + "." + oldName)
            + ", " + singleQuoteWrap(newName) + ", 'COLUMN'");
    }

    @Override
    protected String buildUpdateColumnDefinitionSql(ColumnMetadata columnMetadata) {
        return ALTER_TABLE + quotedTableName() + ALTER_COLUMN
            + getDialect().quoteIdentifier(columnMetadata.getName()) + SPACE + columnMetadata.getDataTypeDefinition();
    }

    @Override
    protected String buildAddConstraintSql(ConstraintType constraintType, ColumnMetadata column) {
        if (constraintType == ConstraintType.NOT_NULL) {
            // SQL Server toggles nullability by re-stating the column type via ALTER COLUMN.
            return ALTER_TABLE + quotedTableName() + ALTER_COLUMN
                + getDialect().quoteIdentifier(column.getName()) + SPACE + column.getDataTypeDefinition() + " NOT NULL";
        }
        return super.buildAddConstraintSql(constraintType, column);
    }

    @Override
    protected String buildDropConstraintSql(ConstraintType constraintType, String name) {
        if (constraintType == ConstraintType.NOT_NULL) {
            ColumnMetadata column = getColumnMetadata(name);
            return ALTER_TABLE + quotedTableName() + ALTER_COLUMN
                + getDialect().quoteIdentifier(column.getName()) + SPACE + column.getDataTypeDefinition() + " NULL";
        }
        // SQL Server drops every named constraint (PK/UNIQUE/CHECK/FK) the same way.
        return ALTER_TABLE + quotedTableName() + " DROP CONSTRAINT " + name;
    }

}
