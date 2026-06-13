package io.github.vatisteve.dataaccess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Identifier quoting must escape an embedded closing delimiter by doubling it (the SQL-standard rule),
 * otherwise a crafted identifier breaks out of its quotes.
 *
 * @author tinhnv
 */
class SqlDialectQuotingTest {

    @Test
    void mariadb_escapesEmbeddedBacktick() {
        assertEquals("`a``b`", new MariadbDialect().quoteIdentifier("a`b"));
        assertEquals("`plain`", new MariadbDialect().quoteIdentifier("plain"));
    }

    @Test
    void postgres_escapesEmbeddedDoubleQuote() {
        assertEquals("\"a\"\"b\"", new PostgresDialect().quoteIdentifier("a\"b"));
        assertEquals("\"plain\"", new PostgresDialect().quoteIdentifier("plain"));
    }

    @Test
    void sqlServer_escapesEmbeddedClosingBracket() {
        assertEquals("[a]]b]", new SqlServerDialect().quoteIdentifier("a]b"));
        assertEquals("[plain]", new SqlServerDialect().quoteIdentifier("plain"));
    }
}
