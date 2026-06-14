package io.github.vatisteve.dataaccess;

/**
 * Microsoft SQL Server dialect: bracket-quoted identifiers, {@code IDENTITY(1,1)} columns, the {@code ON}
 * filegroup storage clause, and {@code OFFSET ... FETCH NEXT} pagination (which is why pagination must be
 * dialect-driven rather than a hardcoded {@code LIMIT/OFFSET}).
 *
 * @author tinhnv
 * @since Jun 13, 2026
 */
public class SqlServerDialect implements SqlDialect {

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
