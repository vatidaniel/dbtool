package io.github.vatisteve.metadata.clickhouse;

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
 * @since Mar 31, 2024
 */
@Slf4j(topic = "DATAGOV.DB_ENGINE.METADATA.CLICKHOUSE_DDL_EXECUTOR")
@RequiredArgsConstructor
@Getter
public class ClickHouseDdlExecutor extends DdlQueryConstants implements DdlExecutor {

    private final TableMetadata tableMetadata;
    private final Connection connection;

    @Override
    public void createTable() throws SQLException {
        if (collectionNullSafe(tableMetadata.getColumnsMetadata()).isEmpty()) {
            throw new IllegalStateException("Cannot create table " + tableMetadata.getName() + " without any column");
        }
        List<String> cols = new ArrayList<>();
        List<String> prs = new ArrayList<>(); // primary key columns
        Map<String, Integer> ordsM = new HashMap<>(); // order by columns
        Map<String, String> ctsM = new HashMap<>(); // column check constraints
        collectionNullSafe(tableMetadata.getColumnsMetadata()).forEach(c -> {
            cols.add(buildColumnSql(c));
            if (c.isPrimaryKey()) prs.add(c.getName());
            if (c instanceof ClickHouseColumnMetadata) {
                ClickHouseColumnMetadata clC = (ClickHouseColumnMetadata) c;
                if (clC.getOrderByIndex() > 0) ordsM.put(c.getName(), clC.getOrderByIndex());
            }
            // there is no relationship between two or more tables in clickhouse
            //
            if (c.getCheckConstraint() != null) ctsM.put(c.getName(), c.getCheckConstraint());
        });
        ctsM.forEach((cName, cCheck) -> cols.add(" CONSTRAINT constraint_" + cName + " CHECK " + cName + SPACE + cCheck));
        StringBuilder sql = new StringBuilder("CREATE TABLE ").append(backtickWrap(tableMetadata.getName()))
            .append(roundBracketWrap(String.join(COMMA + SPACE, cols)))
            .append(" ENGINE =").append(Optional.ofNullable(tableMetadata.getTablespace()).orElse(" MergeTree "));
        if (!prs.isEmpty()) {
            sql.append(SPACE).append(PRIMARY_KEY).append(roundBracketWrap(String.join(COMMA + SPACE, prs)));
        }
        if (!ordsM.isEmpty()) {
            List<String> ordsL = ordsM.entrySet().stream().sorted(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).collect(Collectors.toList());
            sql.append(" ORDER BY ").append(roundBracketWrap(String.join(COMMA + SPACE, ordsL)));
        }
        executeSql(sql.toString());
    }

    private String buildColumnSql(ColumnMetadata col) {
        String result = backtickWrap(col.getName()) + SPACE;
        if (col.isNullable()) result = result + " Nullable" + roundBracketWrap(col.getDataType()) + SPACE;
        else result = result + SPACE + col.getDataType() + SPACE;
        // ClickHouse does not support Identity, Unique constraints
        //
        if (col.getColumnDefault() != null) result = result + buildColumnDefault(col.getColumnDefault());
        // More ClickHouse implementation: MATERIALIZED, EPHEMERAL, ALIAS
        return result;
    }

    private String buildColumnDefault(ColumnMetadata.DefaultColumnValue cd) {
        if (cd.getValue() == null) return " DEFAULT " + NULL.trim();
        return " DEFAULT " + Optional.ofNullable(cd.getDataType()).map(d -> {
            if (d instanceof DataType.BasicDataType) {
                return getColumnDefaultValue((DataType.BasicDataType) d, cd);
            } else if (d.getParent() instanceof DataType.BasicDataType) {
                return getColumnDefaultValue((DataType.BasicDataType) d.getParent(), cd);
            }
            return cd.getValue().toString();
        }).orElse(cd.getValue().toString());
    }

    private String getColumnDefaultValue(DataType.BasicDataType d, ColumnMetadata.DefaultColumnValue cd) {
        switch (d) {
            case STRING:
            case SPATIAL:
            case TEMPORAL:
                // literals must be quoted; function defaults should use a null dataType to stay unquoted
                return singleQuoteWrap(cd.getValue().toString());
            case NUMERIC:
            default:
                return cd.getValue().toString();
        }
    }

    @Override
    public void dropTable() throws SQLException {
        executeSql("DROP TABLE " + backtickWrap(tableMetadata.getName()));
    }

    @Override
    public void renameTable(String newName) throws SQLException {
        executeSql("RENAME TABLE " + backtickWrap(tableMetadata.getName()) + " TO " + backtickWrap(newName));
        tableMetadata.setName(newName);
    }

    @Override
    public void addColumn(String columnName) throws SQLException {
        ColumnMetadata colMeta = getColumn(columnName);
        String sql = ALTER_TABLE + backtickWrap(tableMetadata.getName()) + " ADD COLUMN " + buildColumnSql(colMeta) + " FIRST";
        executeSql(sql);
    }

    private ColumnMetadata getColumn(String columnName) {
        return collectionNullSafe(tableMetadata.getColumnsMetadata()).stream()
            .filter(c -> StringUtils.equals(c.getName(), columnName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No column named '" + columnName + "' in table " + tableMetadata.getName()));
    }

    @Override
    public void dropColumn(String columnName) throws SQLException {
        executeSql(ALTER_TABLE + backtickWrap(tableMetadata.getName()) + " DROP COLUMN " + backtickWrap(columnName));
    }

    @Override
    public void renameColumn(String oldName, String newName) throws SQLException {
        executeSql(ALTER_TABLE + backtickWrap(tableMetadata.getName()) + " RENAME COLUMN "
            + backtickWrap(oldName) + " TO " + backtickWrap(newName));
    }

    @Override
    public void updateColumnDefinition(String columnName) throws SQLException {
        ColumnMetadata colM = getColumn(columnName);
        String sql = ALTER_TABLE + backtickWrap(tableMetadata.getName()) + " MODIFY " + buildColumnSql(colM);
        executeSql(sql);
    }

    @Override
    public void addColumnConstraint(ConstraintType constraintType, String columnName) throws SQLException {
        // ClickHouse only supports CHECK constraints; it has no primary/foreign/unique/not-null constraints.
        if (constraintType != ConstraintType.CHECK) {
            throw new UnsupportedOperationException("ClickHouse does not support " + constraintType + " constraints");
        }
        ColumnMetadata colMeta = getColumn(columnName);
        executeSql(ALTER_TABLE + backtickWrap(tableMetadata.getName()) + " ADD CONSTRAINT constraint_" + columnName
            + " CHECK " + columnName + SPACE + colMeta.getCheckConstraint());
    }

    @Override
    public void dropColumnConstraint(ConstraintType constraintType, String constraintName) throws SQLException {
        if (constraintType != ConstraintType.CHECK) {
            throw new UnsupportedOperationException("ClickHouse does not support " + constraintType + " constraints");
        }
        executeSql(ALTER_TABLE + backtickWrap(tableMetadata.getName()) + " DROP CONSTRAINT " + constraintName);
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void logSqlQuery(String sql) {
        log.trace("CLICKHOUSE DDL QUERY: {}", sql);
    }

    @Override
    public void close() {
        // The Connection is supplied by the caller, who owns its lifecycle; this executor does not
        // close it. close() is kept so the executor can still be used in a try-with-resources block.
    }
}
