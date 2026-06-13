package io.github.vatisteve.metadata.core;

import io.github.vatisteve.dataaccess.SqlDialect;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Standard relational DDL executor. The SQL-standard structure (columns, primary/foreign keys, check
 * constraints, defaults) lives here; the parts that genuinely vary between databases are delegated to a
 * {@link SqlDialect} (identifier quoting, identity clause, storage clause). Dialects that need a different
 * statement shape can override the {@code protected} hooks.
 *
 * @author tinhnv
 * @since Jun 13, 2026
 */
@Slf4j
@Getter
public class StandardSqlDdlExecutor extends DdlQueryConstants implements DdlExecutor {

    private final TableMetadata tableMetadata;
    private final Connection connection;
    private final SqlDialect dialect;

    public StandardSqlDdlExecutor(TableMetadata tableMetadata, Connection connection, SqlDialect dialect) {
        this.tableMetadata = tableMetadata;
        this.connection = connection;
        this.dialect = dialect;
    }

    private static void removeTheLastComma(StringBuilder sql) {
        // sql contain 'append(COMMA).append(SPACE)'
        sql.replace(sql.length() - 2, sql.length(), SPACE);
    }

    @Override
    public void createTable() throws SQLException {
        if (collectionNullSafe(tableMetadata.getColumnsMetadata()).isEmpty()) {
            throw new IllegalStateException("Cannot create table " + tableMetadata.getName() + " without any column");
        }
        StringBuilder sql = new StringBuilder("CREATE TABLE ").append(dialect.quoteIdentifier(tableMetadata.getName()))
            .append(OPEN_BRACKET).append(SPACE);
        List<String> prs = new ArrayList<>();
        Map<String, ReferenceMetadata> ref = new HashMap<>();
        collectionNullSafe(tableMetadata.getColumnsMetadata()).forEach(c -> {
            appendColumnSql(sql, c);
            sql.append(COMMA).append(SPACE);
            if (c.isPrimaryKey()) prs.add(c.getName());
            if (c.getReferenceMetadata() != null) ref.put(c.getName(), c.getReferenceMetadata());
        });
        if (!prs.isEmpty()) {
            sql.append(PRIMARY_KEY).append(OPEN_BRACKET)
                .append(prs.stream().map(dialect::quoteIdentifier).collect(Collectors.joining(COMMA + SPACE)))
                .append(CLOSE_BRACKET).append(COMMA).append(SPACE);
        }
        if (!ref.isEmpty()) {
            ref.forEach((columnName, refInfo) -> {
                appendForeignKeyConstraint(sql, columnName, refInfo);
                sql.append(COMMA).append(SPACE);
            });
        }
        removeTheLastComma(sql);
        sql.append(CLOSE_BRACKET);
        sql.append(dialect.tablespaceClause(tableMetadata.getTablespace()));
        executeSql(sql.toString());
    }

    protected void appendColumnSql(StringBuilder sql, ColumnMetadata c) {
        sql.append(dialect.quoteIdentifier(c.getName())).append(SPACE).append(c.getDataType());
        if (!c.isNullable()) sql.append(" NOT NULL ");
        if (c.isIdentity()) sql.append(dialect.autoIncrementClause());
        if (c.getColumnDefault() != null) appendDefaultColumnValue(sql, c.getColumnDefault());
        if (c.getCheckConstraint() != null) {
            sql.append(" CHECK ").append(OPEN_BRACKET).append(dialect.quoteIdentifier(c.getName()))
                .append(SPACE).append(c.getCheckConstraint()).append(CLOSE_BRACKET);
        }
    }

    private void appendDefaultColumnValue(StringBuilder sql, ColumnMetadata.DefaultColumnValue d) {
        sql.append(" DEFAULT ");
        if (d.getValue() == null) {
            sql.append(NULL.trim());
            return;
        }
        Optional.ofNullable(d.getDataType()).ifPresentOrElse(dt -> {
            if (dt instanceof DataType.BasicDataType) {
                doAppendDefaultColumnValue(sql, d, (DataType.BasicDataType) dt);
            } else if (dt.getParent() instanceof DataType.BasicDataType) {
                doAppendDefaultColumnValue(sql, d, (DataType.BasicDataType) dt.getParent());
            } else {
                sql.append(d.getValue());
            }
        }, () -> sql.append(d.getValue()));
    }

    private static void doAppendDefaultColumnValue(StringBuilder sql, ColumnMetadata.DefaultColumnValue d, DataType.BasicDataType basicDataType) {
        switch (basicDataType) {
            case STRING:
            case SPATIAL:
            case TEMPORAL:
                // string, spatial and temporal literals must be quoted; function defaults
                // (e.g. CURRENT_TIMESTAMP) should be passed with a null dataType to stay unquoted
                sql.append(singleQuoteWrap(d.getValue().toString()));
                break;
            case NUMERIC:
                sql.append(d.getValue().toString());
                break;
            default:
                // no other basic data-type categories exist; nothing to append
                break;
        }
    }

    private void appendForeignKeyConstraint(StringBuilder sql, String columnName, ReferenceMetadata ref) {
        // The referenced table/column are quoted as single identifiers; a schema-qualified
        // (schema.table) reference name would need splitting first.
        sql.append(FOREIGN_KEY).append(SPACE).append(OPEN_BRACKET).append(dialect.quoteIdentifier(columnName)).append(CLOSE_BRACKET).append(SPACE)
            .append(REFERENCES).append(SPACE).append(dialect.quoteIdentifier(ref.getTableName())).append(OPEN_BRACKET)
            .append(dialect.quoteIdentifier(ref.getColumnName())).append(CLOSE_BRACKET);
        if (ref.getOnDelete() != null) sql.append(" ON DELETE ").append(ref.getOnDelete().getLabel().toUpperCase());
        if (ref.getOnUpdate() != null) sql.append(" ON UPDATE ").append(ref.getOnUpdate().getLabel().toUpperCase());
    }

    /** The table name quoted for the active dialect; reuse wherever a statement references the table. */
    protected String quotedTableName() {
        return dialect.quoteIdentifier(tableMetadata.getName());
    }

    @Override
    public void dropTable() throws SQLException {
        executeSql("DROP TABLE " + quotedTableName());
    }

    @Override
    public void renameTable(String newName) throws SQLException {
        executeSql("RENAME TABLE " + quotedTableName() + " TO " + dialect.quoteIdentifier(newName));
        tableMetadata.setName(newName);
    }

    @Override
    public void addColumn(String columnName) throws SQLException {
        ColumnMetadata columnMetadata = getColumnMetadata(columnName);
        StringBuilder sql = new StringBuilder(ALTER_TABLE).append(quotedTableName()).append(" ADD COLUMN ");
        appendColumnSql(sql, columnMetadata);
        executeSql(sql.toString());
    }

    protected ColumnMetadata getColumnMetadata(String columnName) {
        return collectionNullSafe(tableMetadata.getColumnsMetadata()).stream()
            .filter(c -> StringUtils.equals(c.getName(), columnName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No column named '" + columnName + "' in table " + tableMetadata.getName()));
    }

    @Override
    public void dropColumn(String columnName) throws SQLException {
        executeSql(ALTER_TABLE + quotedTableName() + " DROP COLUMN " + dialect.quoteIdentifier(columnName));
    }

    @Override
    public void renameColumn(String oldName, String newName) throws SQLException {
        executeSql(ALTER_TABLE + quotedTableName() + " RENAME COLUMN "
            + dialect.quoteIdentifier(oldName) + " TO " + dialect.quoteIdentifier(newName));
    }

    @Override
    public void updateColumnDefinition(String columnName) throws SQLException {
        executeSql(buildUpdateColumnDefinitionSql(getColumnMetadata(columnName)));
    }

    /**
     * Build the statement that changes an existing column's definition. Default is the MySQL/MariaDB
     * {@code ALTER TABLE ... MODIFY <full column definition>} form; dialects with a different shape
     * (e.g. PostgreSQL's {@code ALTER COLUMN ... TYPE ...}) override this.
     */
    protected String buildUpdateColumnDefinitionSql(ColumnMetadata columnMetadata) {
        StringBuilder sql = new StringBuilder(ALTER_TABLE).append(quotedTableName()).append(" MODIFY ");
        appendColumnSql(sql, columnMetadata);
        return sql.toString();
    }

    @Override
    public void addColumnConstraint(ConstraintType constraintType, String columnName) throws SQLException {
        executeSql(buildAddConstraintSql(constraintType, getColumnMetadata(columnName)));
    }

    @Override
    public void dropColumnConstraint(ConstraintType constraintType, String constraintName) throws SQLException {
        executeSql(buildDropConstraintSql(constraintType, constraintName));
    }

    /**
     * Build an {@code ALTER TABLE ... ADD} constraint statement. Defaults are MySQL/MariaDB forms;
     * dialects whose syntax differs (e.g. PostgreSQL's {@code ALTER COLUMN ... SET NOT NULL}) override
     * this.
     */
    protected String buildAddConstraintSql(ConstraintType constraintType, ColumnMetadata column) {
        String table = quotedTableName();
        String col = dialect.quoteIdentifier(column.getName());
        switch (constraintType) {
            case FOREIGN_KEY: {
                // Named constraint keeps the statement valid for both MySQL/MariaDB and PostgreSQL.
                StringBuilder sql = new StringBuilder(ALTER_TABLE).append(table)
                    .append(" ADD CONSTRAINT ").append(constraintName(constraintType, column)).append(SPACE);
                appendForeignKeyConstraint(sql, column.getName(), column.getReferenceMetadata());
                return sql.toString();
            }
            case PRIMARY_KEY:
                return ALTER_TABLE + table + " ADD PRIMARY KEY " + roundBracketWrap(col);
            case UNIQUE:
                return ALTER_TABLE + table + " ADD CONSTRAINT " + constraintName(constraintType, column)
                    + " UNIQUE " + roundBracketWrap(col);
            case CHECK:
                return ALTER_TABLE + table + " ADD CONSTRAINT " + constraintName(constraintType, column)
                    + " CHECK " + roundBracketWrap(col + SPACE + column.getCheckConstraint());
            case NOT_NULL:
                return ALTER_TABLE + table + " MODIFY " + col + SPACE + column.getDataType() + " NOT NULL";
            default:
                throw new UnsupportedOperationException("Unsupported constraint type: " + constraintType);
        }
    }

    /**
     * Build an {@code ALTER TABLE ... DROP} constraint statement. Defaults are MySQL/MariaDB forms;
     * dialects whose syntax differs (e.g. PostgreSQL's {@code DROP CONSTRAINT}) override this. For
     * {@code NOT_NULL} the {@code name} argument is the column name.
     */
    protected String buildDropConstraintSql(ConstraintType constraintType, String name) {
        String table = quotedTableName();
        switch (constraintType) {
            case FOREIGN_KEY:
                return ALTER_TABLE + table + " DROP FOREIGN KEY " + name;
            case PRIMARY_KEY:
                return ALTER_TABLE + table + " DROP PRIMARY KEY";
            case UNIQUE:
                return ALTER_TABLE + table + " DROP INDEX " + name;
            case CHECK:
                return ALTER_TABLE + table + " DROP CHECK " + name;
            case NOT_NULL: {
                ColumnMetadata column = getColumnMetadata(name);
                return ALTER_TABLE + table + " MODIFY " + dialect.quoteIdentifier(column.getName())
                    + SPACE + column.getDataType();
            }
            default:
                throw new UnsupportedOperationException("Unsupported constraint type: " + constraintType);
        }
    }

    /** Generate a deterministic constraint name for adds that don't carry an explicit name. */
    protected String constraintName(ConstraintType constraintType, ColumnMetadata column) {
        String suffix;
        switch (constraintType) {
            case UNIQUE: suffix = "uq"; break;
            case CHECK: suffix = "chk"; break;
            case PRIMARY_KEY: suffix = "pk"; break;
            case FOREIGN_KEY: suffix = "fk"; break;
            default: suffix = "ct";
        }
        return dialect.quoteIdentifier(tableMetadata.getName() + "_" + column.getName() + "_" + suffix);
    }

    @Override
    public void logSqlQuery(String sql) {
        log.trace("SQL: {}", sql);
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }

}
