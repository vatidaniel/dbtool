package io.github.vatisteve.dataaccess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An immutable SQL string with {@code ?} placeholders plus the ordered values to bind to them. Produced
 * by the query builders so values flow through a {@link PreparedStatement} instead of being concatenated
 * into the SQL text.
 *
 * @author tinhnv
 * @since Jun 13, 2026
 */
public final class ParameterizedQuery {

    private final String sql;
    private final List<Object> parameters;

    public ParameterizedQuery(String sql, List<Object> parameters) {
        this.sql = sql;
        this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
    }

    public String getSql() {
        return sql;
    }

    public List<Object> getParameters() {
        return parameters;
    }

    /**
     * Create a {@link PreparedStatement} for this query on the given connection and bind the parameters
     * in order. The caller owns the returned statement and must close it.
     */
    public PreparedStatement prepare(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
        return statement;
    }

    @Override
    public String toString() {
        return "ParameterizedQuery[sql=" + sql + ", parameters=" + parameters + "]";
    }

}
