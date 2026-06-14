package io.github.vatidaniel.metadata.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;

/**
 * @author tinhnv
 * @since Dec 20, 2023
 */
public interface DdlExecutor extends AutoCloseable {

    void createTable() throws SQLException;

    void dropTable() throws SQLException;

    void renameTable(String newName) throws SQLException;

    void addColumn(String columnName) throws SQLException;

    void dropColumn(String columnName) throws SQLException;

    void renameColumn(String oldName, String newName) throws SQLException;

    void updateColumnDefinition(String columnName) throws SQLException;

    void addColumnConstraint(ConstraintType constraintType, String columnName) throws SQLException;

    void dropColumnConstraint(ConstraintType constraintType, String constraintName) throws SQLException;

    Connection getConnection();
    TableMetadata getTableMetadata();
    void logSqlQuery(String sql);

    /**
     * Execute a generated DDL statement on this executor's connection. DDL cannot be parameterized
     * (a {@link java.sql.PreparedStatement} can only bind values, not identifiers, types, or
     * statement structure), so the SQL is assembled by string concatenation. Identifiers and
     * string-literal defaults are quoted/escaped by the executors, but other inputs
     * ({@code dataTypeExtension}, {@code checkConstraint}, a raw {@link DataType#of(String)} keyword,
     * {@code tablespace}) are concatenated verbatim. The caller is therefore responsible for ensuring
     * the {@link TableMetadata} comes from trusted or pre-validated sources, not untrusted end-user
     * input.
     *
     * @param sql the generated DDL statement to run
     */
    default void executeSql(String sql) throws SQLException {
        logSqlQuery(sql);
        try (Statement statement = getConnection().createStatement()) {
            statement.execute(sql);
        }
    }

    /**
     * Run a sequence of DDL operations on this executor's connection inside a single transaction:
     * disables auto-commit, commits on success, rolls back on any failure, and restores the previous
     * auto-commit setting. Opt-in - statements executed outside this method keep their prior behavior.
     * Note that some engines (e.g. MySQL) auto-commit DDL regardless.
     */
    default void runInTransaction(DdlAction action) throws SQLException {
        Connection connection = getConnection();
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            action.run();
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            // A failing rollback must not mask the original failure that triggered it.
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
            }
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    default <T> Collection<T> collectionNullSafe(Collection<T> collection) {
        if (collection == null) return Collections.emptyList();
        return collection;
    }

    /** A sequence of DDL operations to run together, e.g., via {@link #runInTransaction(DdlAction)}. */
    @FunctionalInterface
    interface DdlAction {
        void run() throws SQLException;
    }

}
