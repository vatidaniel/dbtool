package io.github.vatisteve.metadata.core;

import io.github.vatisteve.dataaccess.MariadbDialect;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author tinhnv
 */
class DdlExecutorTransactionTest {

    private DdlExecutor executorWith(Connection connection) {
        return new StandardSqlDdlExecutor(TableMetadata.builder().name("t").build(), connection, new MariadbDialect());
    }

    @Test
    void runInTransaction_disablesAutoCommitCommitsAndRestores() throws Exception {
        List<String> calls = new ArrayList<>();
        DdlExecutor executor = executorWith(fakeConnection(calls));

        executor.runInTransaction(() -> calls.add("action"));

        assertEquals(Arrays.asList("setAutoCommit:false", "action", "commit", "setAutoCommit:true"), calls);
    }

    @Test
    void runInTransaction_rollsBackAndRestoresOnFailure() {
        List<String> calls = new ArrayList<>();
        DdlExecutor executor = executorWith(fakeConnection(calls));

        assertThrows(SQLException.class, () -> executor.runInTransaction(() -> {
            calls.add("action");
            throw new SQLException("boom");
        }));

        assertEquals(Arrays.asList("setAutoCommit:false", "action", "rollback", "setAutoCommit:true"), calls);
    }

    /** A Connection proxy that records the transaction calls and reports auto-commit on by default. */
    private static Connection fakeConnection(List<String> calls) {
        return (Connection) Proxy.newProxyInstance(
            DdlExecutorTransactionTest.class.getClassLoader(),
            new Class[]{Connection.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getAutoCommit":
                        return true;
                    case "setAutoCommit":
                        calls.add("setAutoCommit:" + args[0]);
                        return null;
                    case "commit":
                        calls.add("commit");
                        return null;
                    case "rollback":
                        calls.add("rollback");
                        return null;
                    default:
                        return null;
                }
            });
    }
}
