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

    public static String getInformationSchemaTableName(String tableName) {
        return String.format("`%s`.`%s`", INFORMATION_SCHEMA, tableName);
    }

    /**
     * Format common sql query result result object value to string
     */
    public static String asString(Object object) {
        // TODO: implement the as-string method
        return object.toString();
    }

    public String describeColumnsNameQuery(String schemaName, String tableName, String... ignoredColumns) {
        return new BasicQuery().select(columnDescriptions)
            .from(getInformationSchemaTableName(COLUMNS_TABLE))
            .where(new BasicCriteria("table_schema").equalWithSingleQuote(schemaName)
                .and("table_name").equalWithSingleQuote(tableName)
                .and(new BasicCriteria("column_name").notInFormat(ignoredColumns).toCriteriaString()))
            .toQueryString();
    }

    public String buildCountQuery(String tableName) {
        return new BasicQuery().countAll().from(tableName).toQueryString();
    }

    public String buildFetchDataQuery(String tableName, String[] columnNames, int limit, long offset) {
        return new BasicQuery().select(columnNames).from(tableName).limit(limit).offset(offset).toQueryString();
    }

    public String buildFetchDataWithCountQuery(String tableName, String[] columnNames, int limit, long offset) {
        List<String> columnsNameWithCountFormat = new ArrayList<>(Arrays.asList(columnNames));
        columnsNameWithCountFormat.add(String.format(SqlQueryConstants.COUNT_FUNCTION_FORMAT, ""));
        return new BasicQuery().select(columnsNameWithCountFormat.toArray(new String[0]))
            .from(tableName).limit(limit).offset(offset).toQueryString();
    }
}
