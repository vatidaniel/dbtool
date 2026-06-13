package io.github.vatisteve.dataaccess;

/**
 * @author tinhnv
 * @since Oct 31, 2023
 *
 * @apiNote used for mariadb, mysql query format. Create interfaces for more implementation
 */
// TODO more flexible, separate the select, where and other clauses before building sql query
public class BasicQuery extends SqlQueryConstants {

    private final StringBuilder sqlQuery;
    private final SqlDialect dialect;

    private boolean selectClauseIsUsed = false;

    public BasicQuery() {
        this(new MariadbDialect());
    }

    public BasicQuery(SqlDialect dialect) {
        this.sqlQuery = new StringBuilder();
        this.dialect = dialect;
    }

    private void throwQueryBuilderException() {
        throw new QueryBuilderException(String.format("There are conflicts when building SQL query: %s", sqlQuery));
    }

    // Select ...
    public BasicQuery select(String... elements) {
        sqlQuery.append(SELECT_KEYWORD).append(String.join(COMMA + SPACE, elements)).append(SPACE);
        if (selectClauseIsUsed) throwQueryBuilderException();
        selectClauseIsUsed = true;
        return this;
    }

    // Select ...
    public BasicQuery selectMax(String element) {
        sqlQuery.append(SELECT_KEYWORD).append(String.format(MAX_FORMAT, element)).append(SPACE);
        if (selectClauseIsUsed) throwQueryBuilderException();
        selectClauseIsUsed = true;
        return this;
    }

    // Select ...
    public BasicQuery countAll() {
        sqlQuery.append(SELECT_KEYWORD).append(COUNT_ALL).append(SPACE);
        if (selectClauseIsUsed) throwQueryBuilderException();
        selectClauseIsUsed = true;
        return this;
    }

    // From ...
    public BasicQuery from(String table) {
        sqlQuery.append(FROM_KEYWORD).append(table).append(SPACE);
        return this;
    }

    // Join ... (inner join)
    public BasicQuery innerJoin(String table) {
        sqlQuery.append(INNER_JOIN_KEYWORD).append(table).append(SPACE);
        return this;
    }

    // Join on ...
    public BasicQuery on(String leftTable, String rightTable, String onColumn) {
        sqlQuery.append(ON).append(leftTable).append(DOT).append(onColumn).append(EQUAL)
            .append(rightTable).append(DOT).append(onColumn).append(SPACE);
        return this;
    }

    // ...
    public BasicQuery append(String appendQuery) {
        sqlQuery.append(appendQuery);
        return this;
    }

    // Where ...
    public BasicQuery where(BasicCriteria criteria) {
        sqlQuery.append(WHERE_KEYWORD).append(criteria.toCriteriaString()).append(SPACE);
        return this;
    }

    // Limit ... (raw MySQL/Postgres primitive; not dialect-aware - prefer paginate(...))
    public BasicQuery limit(int limit) {
        sqlQuery.append(LIMIT_KEYWORD).append(limit).append(SPACE);
        return this;
    }

    // Offset ... (raw MySQL/Postgres primitive; not dialect-aware - prefer paginate(...))
    public BasicQuery offset(long offset) {
        sqlQuery.append(OFFSET_KEYWORD).append(offset).append(SPACE);
        return this;
    }

    // Pagination, rendered by the dialect (e.g. LIMIT/OFFSET vs OFFSET ... FETCH NEXT)
    public BasicQuery paginate(int limit, long offset) {
        sqlQuery.append(dialect.paginate(limit, offset));
        return this;
    }

    public String toQueryString() {
        return sqlQuery.toString();
    }

    @Override
    public String toString() {
        return "Basic Query Builder: [" + toQueryString() + "]";
    }

}
