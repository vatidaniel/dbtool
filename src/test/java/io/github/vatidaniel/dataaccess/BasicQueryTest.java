package io.github.vatidaniel.dataaccess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author tinhnv
 */
class BasicQueryTest {

    @Test
    void select_from_where_limit_offset() {
        String query = new BasicQuery()
            .select("id", "name")
            .from("`users`")
            .where(new BasicCriteria("age").equalWithNumber(30))
            .limit(10)
            .offset(20)
            .toQueryString();
        assertTrue(query.startsWith("SELECT id, name "), query);
        assertTrue(query.contains("FROM `users`"), query);
        assertTrue(query.contains("WHERE age = 30"), query);
        assertTrue(query.contains("LIMIT 10"), query);
        assertTrue(query.contains("OFFSET 20"), query);
    }

    @Test
    void countAll_emitsCountExpression() {
        String query = new BasicQuery().countAll().from("`users`").toQueryString();
        assertTrue(query.contains(SqlQueryConstants.COUNT_ALL), query);
    }

    @Test
    void innerJoinOn_buildsJoinClause() {
        String query = new BasicQuery()
            .select("a.id")
            .from("`a`")
            .innerJoin("`b`")
            .on("`a`", "`b`", "id")
            .toQueryString();
        assertTrue(query.contains("INNER JOIN `b`"), query);
        assertTrue(query.contains("`a`.id=`b`.id"), query);
    }

    @Test
    void usingSelectTwice_throwsQueryBuilderException() {
        BasicQuery query = new BasicQuery().select("id");
        assertThrows(QueryBuilderException.class, () -> query.select("name"));
    }

    @Test
    void multipleJoins_eachOnAttachesToItsJoin() {
        String query = new BasicQuery()
            .select("*")
            .from("`a`")
            .innerJoin("`b`").on("`a`", "`b`", "id")
            .innerJoin("`c`").on("`a`.x = `c`.y")
            .toQueryString();
        assertTrue(query.contains("INNER JOIN `b`  ON `a`.id=`b`.id"), query);
        assertTrue(query.contains("INNER JOIN `c`  ON `a`.x = `c`.y"), query);
        // second join must appear after the first join's ON condition
        assertTrue(query.indexOf("INNER JOIN `c`") > query.indexOf("`a`.id=`b`.id"), query);
    }

    @Test
    void onBeforeInnerJoin_throwsQueryBuilderException() {
        BasicQuery query = new BasicQuery().select("*").from("`a`");
        assertThrows(QueryBuilderException.class, () -> query.on("`a`.id = `b`.id"));
    }

    @Test
    void on_joinsOnDifferingColumnNames() {
        String query = new BasicQuery()
            .select("*")
            .from("`a`")
            .innerJoin("`b`").on("`a`", "id", "`b`", "a_id")
            .toQueryString();
        assertTrue(query.contains("`a`.id=`b`.a_id"), query);
    }

    @Test
    void emptyQuery_isEmptyString() {
        assertEquals("", new BasicQuery().toQueryString());
    }

    @Test
    void paginate_defaultDialect_usesLimitOffset() {
        String query = new BasicQuery().select("*").from("`t`").paginate(10, 20).toQueryString();
        assertTrue(query.contains("LIMIT 10 OFFSET 20"), query);
    }

    @Test
    void paginate_isDialectAware() {
        // Swapping the dialect changes the pagination clause with no other code change.
        String query = new BasicQuery(SqlServerDialect.INSTANCE).select("*").from("[t]").paginate(10, 20).toQueryString();
        assertTrue(query.contains("OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY"), query);
        assertTrue(!query.contains("LIMIT"), query);
    }

    @Test
    void clausesAssembleInSqlOrder_regardlessOfCallOrder() {
        // Call clauses out of order; they must still render SELECT ... FROM ... WHERE ... in SQL order.
        String query = new BasicQuery()
            .where(new BasicCriteria("age").equalWithNumber(30))
            .from("`users`")
            .select("id")
            .toQueryString();
        int select = query.indexOf("SELECT");
        int from = query.indexOf("FROM");
        int where = query.indexOf("WHERE");
        assertTrue(select >= 0 && select < from && from < where, query);
    }

    @Test
    void orderBy_precedesPagination_forValidOffsetFetch() {
        String query = new BasicQuery(SqlServerDialect.INSTANCE)
            .select("*").from("[t]").orderBy("[id]").paginate(10, 20).toQueryString();
        int orderBy = query.indexOf("ORDER BY");
        int offset = query.indexOf("OFFSET");
        assertTrue(query.contains("ORDER BY [id]"), query);
        assertTrue(orderBy >= 0 && orderBy < offset, query);
        assertTrue(query.contains("OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY"), query);
    }

    @Test
    void toPreparedQuery_collectsWhereParametersInOrder() {
        ParameterizedQuery pq = new BasicQuery()
            .select("id")
            .from("`users`")
            .where(new BasicCriteria("age").equal(30).and(new BasicCriteria("city").in("NY", "LA")))
            .paginate(10, 0)
            .toPreparedQuery();
        assertTrue(pq.getSql().contains("WHERE age = ?"), pq.getSql());
        assertTrue(pq.getSql().contains("IN ( ?, ? )"), pq.getSql());
        // pagination uses literal ints, so it contributes no parameters
        assertEquals(java.util.Arrays.asList(30, "NY", "LA"), pq.getParameters());
    }
}
