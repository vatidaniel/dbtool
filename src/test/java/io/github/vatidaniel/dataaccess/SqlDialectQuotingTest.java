package io.github.vatidaniel.dataaccess;

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
        assertEquals("`a``b`", MariadbDialect.INSTANCE.quoteIdentifier("a`b"));
        assertEquals("`plain`", MariadbDialect.INSTANCE.quoteIdentifier("plain"));
    }

    @Test
    void postgres_escapesEmbeddedDoubleQuote() {
        assertEquals("\"a\"\"b\"", PostgresDialect.INSTANCE.quoteIdentifier("a\"b"));
        assertEquals("\"plain\"", PostgresDialect.INSTANCE.quoteIdentifier("plain"));
    }

    @Test
    void sqlServer_escapesEmbeddedClosingBracket() {
        assertEquals("[a]]b]", SqlServerDialect.INSTANCE.quoteIdentifier("a]b"));
        assertEquals("[plain]", SqlServerDialect.INSTANCE.quoteIdentifier("plain"));
    }
}
