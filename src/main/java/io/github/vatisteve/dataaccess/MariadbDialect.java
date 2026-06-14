package io.github.vatisteve.dataaccess;

/**
 * MySQL / MariaDB dialect: backtick-quoted identifiers, {@code AUTO_INCREMENT} identity columns and the
 * {@code Engine =} storage clause.
 *
 * @author tinhnv
 * @since Jun 13, 2026
 */
public class MariadbDialect implements SqlDialect {

    /** Shared instance; the dialect is stateless, so a single instance can be reused. */
    public static final MariadbDialect INSTANCE = new MariadbDialect();

    @Override
    public String quoteIdentifier(String identifier) {
        return SqlQueryConstants.backtickWrap(identifier);
    }

    @Override
    public String autoIncrementClause() {
        return " AUTO_INCREMENT ";
    }

    @Override
    public String tablespaceClause(String tablespace) {
        return tablespace == null ? "" : " Engine = " + tablespace;
    }

    @Override
    public String paginate(int limit, long offset) {
        return "LIMIT " + limit + " OFFSET " + offset + " ";
    }

}
