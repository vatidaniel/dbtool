package io.github.vatisteve.metadata.mariadb;

import io.github.vatisteve.metadata.core.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author tinhnv
 * @since Dec 20, 2023
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public class MariadbDdlExecutor extends DdlQueryConstants implements DdlExecutor {

    private final TableMetadata tableMetadata;
    private final Connection connection;

    private static void removeTheLastComma(StringBuilder sql) {
        // sql contain 'append(COMMA).append(SPACE)'
        sql.replace(sql.length() - 2, sql.length(), SPACE);
    }

    @Override
    public void createTable() throws SQLException {
        StringBuilder sql = new StringBuilder("CREATE TABLE ").append(backtickWrap(tableMetadata.getName()))
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
                .append(prs.stream().map(DdlQueryConstants::backtickWrap).collect(Collectors.joining(COMMA + SPACE)))
                .append(CLOSE_BRACKET).append(COMMA).append(SPACE);
        }
        if (!ref.isEmpty()) {
            ref.forEach((columnName, refInfo) -> {
                appendForeignKeyConstraint(sql, backtickWrap(columnName), refInfo);
                sql.append(COMMA).append(SPACE);
            });
        }
        removeTheLastComma(sql);
        sql.append(CLOSE_BRACKET);
        if (tableMetadata.getTablespace() != null) sql.append(" Engine = ").append(tableMetadata.getTablespace());
        //...
        executeSql(sql.toString());
    }

    private void appendColumnSql(StringBuilder sql, ColumnMetadata c) {
        sql.append(backtickWrap(c.getName())).append(SPACE).append(c.getDataType());
        if (!c.isNullable()) sql.append(" NOT NULL ");
        if (c.isIdentity()) sql.append(" AUTO_INCREMENT ");
        if (c.getColumnDefault() != null) appendDefaultColumnValue(sql, c.getColumnDefault());
        if (c.getCheckConstraint() != null) {
            sql.append(" CHECK ").append(OPEN_BRACKET).append(backtickWrap(c.getName()))
                .append(SPACE).append(c.getCheckConstraint()).append(CLOSE_BRACKET);
        }
    }

    private void appendDefaultColumnValue(StringBuilder sql, ColumnMetadata.DefaultColumnValue d) {
        sql.append(" DEFAULT ");
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
                sql.append(singleQuoteWrap(d.getValue().toString()));
                break;
            case NUMERIC:
            case TEMPORAL:
                sql.append(d.getValue().toString());
                break;
            default: //...
        }
    }

    private void appendForeignKeyConstraint(StringBuilder sql, String cn, ReferenceMetadata ref) {
        sql.append(FOREIGN_KEY).append(SPACE).append(OPEN_BRACKET).append(cn).append(CLOSE_BRACKET).append(SPACE)
            .append(REFERENCES).append(SPACE).append(ref.getTableName()).append(OPEN_BRACKET)
            .append(ref.getColumnName()).append(CLOSE_BRACKET);
        if (ref.getOnDelete() != null) sql.append(" ON DELETE ").append(ref.getOnDelete().getLabel().toUpperCase());
        if (ref.getOnUpdate() != null) sql.append(" ON UPDATE ").append(ref.getOnUpdate().getLabel().toUpperCase());
    }

    @Override
    public void dropTable() throws SQLException {
        executeSql("DROP TABLE " + tableMetadata.getName());
    }

    @Override
    public void renameTable(String newName) throws SQLException {
        executeSql("RENAME TABLE " + tableMetadata.getName() + " TO " + newName);
        tableMetadata.setName(newName);
    }

    @Override
    public void addColumn(String columnName) throws SQLException {
        ColumnMetadata columnMetadata = getColumnMetadata(columnName);
        StringBuilder sql = new StringBuilder(ALTER_TABLE).append(tableMetadata.getName()).append(" ADD COLUMN ");
        appendColumnSql(sql, columnMetadata);
        executeSql(sql.toString());
    }

    private ColumnMetadata getColumnMetadata(String columnName) {
        return collectionNullSafe(tableMetadata.getColumnsMetadata()).stream()
            .filter(c -> StringUtils.equals(c.getName(), columnName))
            .findFirst().orElseThrow();
    }

    @Override
    public void dropColumn(String columnName) throws SQLException {
        executeSql(ALTER_TABLE + tableMetadata.getName() + " DROP COLUMN " + columnName);
    }

    @Override
    public void renameColumn(String oldName, String newName) throws SQLException {
        executeSql(ALTER_TABLE + tableMetadata.getName() + " RENAME COLUMN " + oldName + " TO " + newName);
    }

    @Override
    public void updateColumnDefinition(String columnName) throws SQLException {
        ColumnMetadata columnMetadata = getColumnMetadata(columnName);
        StringBuilder sql = new StringBuilder(ALTER_TABLE).append(tableMetadata.getName()).append(" MODIFY ");
        appendColumnSql(sql, columnMetadata);
        executeSql(sql.toString());
    }

    @Override
    public void addColumnConstraint(ConstraintType constraintType, String columnName) throws SQLException {
        ColumnMetadata c = getColumnMetadata(columnName);
        StringBuilder sql = new StringBuilder(ALTER_TABLE + tableMetadata.getName() + " ADD CONSTRAINT ");
        switch (constraintType) {
            case FOREIGN_KEY: {
                appendForeignKeyConstraint(sql, columnName, c.getReferenceMetadata());
                break;
            }
            case PRIMARY_KEY:
            case CHECK:
            case UNIQUE:
            case NOT_NULL:
                // implement for the rest of constraint types
                throw new UnsupportedOperationException("Create " + constraintType + " constraint: The function has not been implemented yet!");
        }
        executeSql(sql.toString());
    }

    @Override
    public void dropColumnConstraint(ConstraintType constraintType, String constraintName) throws SQLException {
        String sql = ALTER_TABLE + tableMetadata.getName() + " DROP ";
        switch (constraintType) {
            case FOREIGN_KEY: {
                sql += FOREIGN_KEY + SPACE + constraintName;
                break;
            }
            case PRIMARY_KEY:
            case CHECK:
            case UNIQUE:
            case NOT_NULL:
                // implement for the rest of constraint types
                throw new UnsupportedOperationException("Drop " + constraintType + " constraint: The function has not been implemented yet!");
        }
        executeSql(sql);
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
