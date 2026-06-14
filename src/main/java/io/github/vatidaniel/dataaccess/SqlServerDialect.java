package io.github.vatidaniel.dataaccess;

/**
 * Microsoft SQL Server dialect: bracket-quoted identifiers, {@code IDENTITY(1,1)} columns, the {@code ON}
 * filegroup storage clause, and {@code OFFSET ... FETCH NEXT} pagination (which is why pagination must be
 * dialect-driven rather than a hardcoded {@code LIMIT/OFFSET}).
 *
 * @author tinhnv
 * @since Jun 13, 2026
 */
public enum SqlServerDialect implements SqlDialect {

    /** The single shared instance; the dialect is stateless, so one instance is reused everywhere. */
    INSTANCE;

    @Override
    public String quoteIdentifier(String identifier) {
        return SqlQueryConstants.quoteIdentifier(identifier, "[", "]");
    }

    @Override
    public String autoIncrementClause() {
        return " IDENTITY(1,1) ";
    }

    @Override
    public String tablespaceClause(String tablespace) {
        return tablespace == null ? "" : " ON " + tablespace;
    }

    @Override
    public String paginate(int limit, long offset) {
        return "OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY ";
    }

}
