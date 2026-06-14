package io.github.vatidaniel.dataaccess;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author tinhnv
 */
class ParameterizedQueryTest {

    @Test
    void exposesSqlAndParameters() {
        ParameterizedQuery q = new ParameterizedQuery("SELECT * FROM t WHERE a = ?", Arrays.asList("x"));
        assertEquals("SELECT * FROM t WHERE a = ?", q.getSql());
        assertEquals(Arrays.asList("x"), q.getParameters());
    }

    @Test
    void parametersAreDefensivelyCopiedAndUnmodifiable() {
        List<Object> source = new ArrayList<>(Arrays.asList(1));
        ParameterizedQuery q = new ParameterizedQuery("x", source);
        source.add(2); // mutating the source must not affect the query
        assertEquals(Arrays.asList(1), q.getParameters());
        assertThrows(UnsupportedOperationException.class, () -> q.getParameters().add(99));
    }

    @Test
    void prepareBindsParametersInOrder() throws Exception {
        ParameterizedQuery q = new ParameterizedQuery(
            "SELECT * FROM t WHERE a = ? AND b = ?", Arrays.asList("x", 42));

        String[] preparedSql = new String[1];
        List<Object[]> bound = new ArrayList<>();
        Connection connection = fakeConnection(preparedSql, bound);

        q.prepare(connection);

        assertEquals("SELECT * FROM t WHERE a = ? AND b = ?", preparedSql[0]);
        assertEquals(2, bound.size());
        assertArrayEquals(new Object[]{1, "x"}, bound.get(0));
        assertArrayEquals(new Object[]{2, 42}, bound.get(1));
    }

    /** A JDBC stub built with dynamic proxies: records the prepared SQL and every setObject(index, value). */
    private static Connection fakeConnection(String[] preparedSql, List<Object[]> bound) {
        ClassLoader cl = ParameterizedQueryTest.class.getClassLoader();
        PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(cl,
            new Class[]{PreparedStatement.class}, (proxy, method, args) -> {
                if ("setObject".equals(method.getName())) {
                    bound.add(new Object[]{args[0], args[1]});
                    return null;
                }
                return defaultValue(method.getReturnType());
            });
        return (Connection) Proxy.newProxyInstance(cl, new Class[]{Connection.class},
            (proxy, method, args) -> {
                if ("prepareStatement".equals(method.getName())) {
                    preparedSql[0] = (String) args[0];
                    return statement;
                }
                return defaultValue(method.getReturnType());
            });
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
}
