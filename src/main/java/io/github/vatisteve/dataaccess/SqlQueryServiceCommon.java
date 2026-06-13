package io.github.vatisteve.dataaccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author tinhnv
 * @since Oct 31, 2023
 */
public class SqlQueryServiceCommon {

    public static final String INFORMATION_SCHEMA = "information_schema";
    public static final String COLUMNS_TABLE = "columns";

    protected static final String[] columnDescriptions = {"ordinal_position", "column_name", "data_type"};

    private final SqlDialect dialect;

    public SqlQueryServiceCommon() {
        this(new MariadbDialect());
    }

    public SqlQueryServiceCommon(SqlDialect dialect) {
        this.dialect = dialect;
    }

    public static String getInformationSchemaTableName(String tableName) {
        return String.format("`%s`.`%s`", INFORMATION_SCHEMA, tableName);
    }

    private String informationSchemaTable(String tableName) {
        return dialect.quoteIdentifier(INFORMATION_SCHEMA) + SqlQueryConstants.DOT + dialect.quoteIdentifier(tableName);
    }

    /**
     * Format a common SQL query result value to string. {@code null} becomes an empty string and
     * character/byte arrays are rendered as their textual content rather than the default array
     * {@code toString()}.
     */
    public static String asString(Object object) {
        if (object == null) return "";
        if (object instanceof char[]) return new String((char[]) object);
        if (object instanceof byte[]) return new String((byte[]) object);
        return String.valueOf(object);
    }

    public String describeColumnsNameQuery(String schemaName, String tableName, String... ignoredColumns) {
        return new BasicQuery().select(columnDescriptions)
            .from(informationSchemaTable(COLUMNS_TABLE))
            .where(new BasicCriteria("table_schema").equalWithSingleQuote(schemaName)
                .and("table_name").equalWithSingleQuote(tableName)
                .and(new BasicCriteria("column_name").notInFormat(ignoredColumns).toCriteriaString()))
            .toQueryString();
    }

    public String buildCountQuery(String tableName) {
        return new BasicQuery().countAll().from(tableName).toQueryString();
    }

    public String buildFetchDataQuery(String tableName, String[] columnNames, int limit, long offset) {
        return new BasicQuery(dialect).select(columnNames).from(tableName)
            .paginate(limit, offset).toQueryString();
    }

    public String buildFetchDataWithCountQuery(String tableName, String[] columnNames, int limit, long offset) {
        List<String> columnsNameWithCountFormat = new ArrayList<>(Arrays.asList(columnNames));
        columnsNameWithCountFormat.add(String.format(SqlQueryConstants.COUNT_FUNCTION_FORMAT, ""));
        return new BasicQuery(dialect).select(columnsNameWithCountFormat.toArray(new String[0]))
            .from(tableName).paginate(limit, offset).toQueryString();
    }
}
