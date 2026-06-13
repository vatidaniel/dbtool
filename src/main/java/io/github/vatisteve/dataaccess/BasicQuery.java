package io.github.vatisteve.dataaccess;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tinhnv
 * @since Oct 31, 2023
 *
 * @apiNote used for mariadb, mysql query format. Create interfaces for more implementation
 *
 * <p>Clauses are collected independently and assembled in SQL order by {@link #toQueryString()}, so the
 * builder methods may be called in any order. Each join is held as its own entry, and {@link #on} attaches
 * its condition to the most recent {@link #innerJoin}, so several joins can be added without an ordering
 * trap. The raw {@link #append(String)} escape hatch always lands at the very end of the query, regardless
 * of when it is called.
 */
public class BasicQuery extends SqlQueryConstants {

    private final SqlDialect dialect;

    private String selectClause;
    private String fromClause;
    private final List<StringBuilder> joins = new ArrayList<>();
    private String whereClause;
    private final StringBuilder paginationClause = new StringBuilder();
    private final StringBuilder trailing = new StringBuilder();

    public BasicQuery() {
        this(new MariadbDialect());
    }

    public BasicQuery(SqlDialect dialect) {
        this.dialect = dialect;
    }

    private void setSelectClause(String clause) {
        if (selectClause != null) {
            throw new QueryBuilderException(String.format("There are conflicts when building SQL query: %s", clause));
        }
        selectClause = clause;
    }

    // Select ...
    public BasicQuery select(String... elements) {
        setSelectClause(SELECT_KEYWORD + String.join(COMMA + SPACE, elements) + SPACE);
        return this;
    }

    // Select MAX(...)
    public BasicQuery selectMax(String element) {
        setSelectClause(SELECT_KEYWORD + String.format(MAX_FORMAT, element) + SPACE);
        return this;
    }

    // Select COUNT(1)
    public BasicQuery countAll() {
        setSelectClause(SELECT_KEYWORD + COUNT_ALL + SPACE);
        return this;
    }

    // From ...
    public BasicQuery from(String table) {
        fromClause = FROM_KEYWORD + table + SPACE;
        return this;
    }

    // Join ... (inner join) - starts a new join entry that on(...) completes
    public BasicQuery innerJoin(String table) {
        joins.add(new StringBuilder(INNER_JOIN_KEYWORD).append(table).append(SPACE));
        return this;
    }

    // Join on ... with a free-form condition, attached to the most recent join
    public BasicQuery on(String condition) {
        lastJoin().append(ON).append(condition).append(SPACE);
        return this;
    }

    // Join on ... convenience for leftTable.onColumn = rightTable.onColumn (same column name on both sides)
    public BasicQuery on(String leftTable, String rightTable, String onColumn) {
        return on(leftTable, onColumn, rightTable, onColumn);
    }

    // Join on ... convenience for leftTable.leftColumn = rightTable.rightColumn (differing column names)
    public BasicQuery on(String leftTable, String leftColumn, String rightTable, String rightColumn) {
        lastJoin().append(ON).append(leftTable).append(DOT).append(leftColumn).append(EQUAL)
            .append(rightTable).append(DOT).append(rightColumn).append(SPACE);
        return this;
    }

    private StringBuilder lastJoin() {
        if (joins.isEmpty()) {
            throw new QueryBuilderException("on(...) called before innerJoin(...)");
        }
        return joins.get(joins.size() - 1);
    }

    // Append raw text to the end of the query
    public BasicQuery append(String appendQuery) {
        trailing.append(appendQuery);
        return this;
    }

    // Where ...
    public BasicQuery where(BasicCriteria criteria) {
        whereClause = WHERE_KEYWORD + criteria.toCriteriaString() + SPACE;
        return this;
    }

    // Limit ... (raw MySQL/Postgres primitive; not dialect-aware - prefer paginate(...))
    public BasicQuery limit(int limit) {
        paginationClause.append(LIMIT_KEYWORD).append(limit).append(SPACE);
        return this;
    }

    // Offset ... (raw MySQL/Postgres primitive; not dialect-aware - prefer paginate(...))
    public BasicQuery offset(long offset) {
        paginationClause.append(OFFSET_KEYWORD).append(offset).append(SPACE);
        return this;
    }

    // Pagination, rendered by the dialect (e.g. LIMIT/OFFSET vs OFFSET ... FETCH NEXT)
    public BasicQuery paginate(int limit, long offset) {
        paginationClause.append(dialect.paginate(limit, offset));
        return this;
    }

    public String toQueryString() {
        StringBuilder sql = new StringBuilder();
        if (selectClause != null) sql.append(selectClause);
        if (fromClause != null) sql.append(fromClause);
        joins.forEach(sql::append);
        if (whereClause != null) sql.append(whereClause);
        sql.append(paginationClause);
        sql.append(trailing);
        return sql.toString();
    }

    @Override
    public String toString() {
        return "Basic Query Builder: [" + toQueryString() + "]";
    }

}
