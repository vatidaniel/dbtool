package io.github.vatisteve.dataaccess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author tinhnv
 */
class SqlQueryServiceCommonTest {

    private final SqlQueryServiceCommon service = new SqlQueryServiceCommon();

    @Test
    void buildFetchDataWithCountQuery_doesNotThrowAndContainsColumnsAndCount() {
        String[] columns = {"id", "name"};
        // Regression: previously threw UnsupportedOperationException because the
        // backing list from Arrays.asList(...) is fixed-size.
        String query = assertDoesNotThrow(
            () -> service.buildFetchDataWithCountQuery("`my_table`", columns, 10, 0));
        assertTrue(query.contains("id"), query);
        assertTrue(query.contains("name"), query);
        assertTrue(query.contains("COUNT(1) OVER"), query);
        assertTrue(query.contains("`my_table`"), query);
    }

    @Test
    void buildCountQuery_selectsCountFromTable() {
        String query = service.buildCountQuery("`my_table`");
        assertTrue(query.contains(SqlQueryConstants.COUNT_ALL), query);
        assertTrue(query.contains("FROM `my_table`"), query);
    }

    @Test
    void buildFetchDataQuery_appliesLimitAndOffset() {
        String query = service.buildFetchDataQuery("`my_table`", new String[]{"id"}, 25, 50);
        assertTrue(query.contains("LIMIT 25"), query);
        assertTrue(query.contains("OFFSET 50"), query);
    }

    @Test
    void describeColumnsNameQuery_targetsInformationSchema() {
        String query = service.describeColumnsNameQuery("my_schema", "my_table", "ignored");
        assertTrue(query.contains("`information_schema`.`columns`"), query);
        assertTrue(query.contains("'my_schema'"), query);
        assertTrue(query.contains("'my_table'"), query);
        assertTrue(query.contains("NOT IN"), query);
    }

    @Test
    void describeColumnsNameQuery_usesDialectIdentifierQuoting() {
        SqlQueryServiceCommon pg = new SqlQueryServiceCommon(new PostgresDialect());
        String query = pg.describeColumnsNameQuery("my_schema", "my_table");
        // PostgreSQL double-quotes identifiers instead of MySQL backticks
        assertTrue(query.contains("\"information_schema\".\"columns\""), query);
        assertTrue(!query.contains("`"), query);
    }
}
