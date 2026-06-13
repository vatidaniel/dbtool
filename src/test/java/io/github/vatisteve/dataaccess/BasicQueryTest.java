package io.github.vatisteve.dataaccess;

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
        String query = new BasicQuery(new SqlServerDialect()).select("*").from("[t]").paginate(10, 20).toQueryString();
        assertTrue(query.contains("OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY"), query);
        assertTrue(!query.contains("LIMIT"), query);
    }
}
