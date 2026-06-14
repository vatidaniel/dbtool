package io.github.vatisteve.metadata.clickhouse;

import io.github.vatisteve.metadata.core.ColumnMetadata;
import io.github.vatisteve.metadata.core.ConstraintType;
import io.github.vatisteve.metadata.core.TableMetadata;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author tinhnv
 */
class ClickHouseDdlExecutorTest {

    private static class CapturingExecutor extends ClickHouseDdlExecutor {
        private String capturedSql;

        CapturingExecutor(TableMetadata tableMetadata) {
            super(tableMetadata, (Connection) null);
        }

        @Override
        public void executeSql(String sql) {
            this.capturedSql = sql;
        }
    }

    @Test
    void createTable_wrapsNullableColumnsAndDefaultsEngineToMergeTree() throws Exception {
        ColumnMetadata id = ClickHouseColumnMetadata.builder()
            .name("id").dataType(ClickHouseDataType.INT_64).nullable(false).primaryKey(true).orderByIndex(1)
            .build();
        ColumnMetadata name = ClickHouseColumnMetadata.builder()
            .name("name").dataType(ClickHouseDataType.CH_STRING).nullable(true)
            .build();
        TableMetadata table = TableMetadata.builder()
            .name("events")
            .columnsMetadata(List.of(id, name))
            .build();

        CapturingExecutor executor = new CapturingExecutor(table);
        executor.createTable();
        String sql = executor.capturedSql;

        assertNotNull(sql);
        assertTrue(sql.contains("CREATE TABLE `events`"), sql);
        // non-nullable column stays bare, nullable column is wrapped
        assertTrue(sql.contains("`id`  Int64"), sql);
        assertTrue(sql.contains("`name`  Nullable(String)"), sql);
        assertTrue(sql.contains("ENGINE = MergeTree"), sql);
        assertTrue(sql.contains("PRIMARY KEY(id)"), sql);
        assertTrue(sql.contains("ORDER BY (id)"), sql);
    }

    @Test
    void createTable_honorsExplicitTablespaceAsEngine() throws Exception {
        ColumnMetadata col = ClickHouseColumnMetadata.builder()
            .name("v").dataType(ClickHouseDataType.CH_STRING).nullable(false)
            .build();
        TableMetadata table = TableMetadata.builder()
            .name("t")
            .tablespace(" Log ")
            .columnsMetadata(List.of(col))
            .build();

        CapturingExecutor executor = new CapturingExecutor(table);
        executor.createTable();

        assertTrue(executor.capturedSql.contains("ENGINE = Log"), executor.capturedSql);
    }

    private CapturingExecutor checkExecutor() {
        ColumnMetadata age = ClickHouseColumnMetadata.builder()
            .name("age").dataType(ClickHouseDataType.INT_32).checkConstraint("> 0").build();
        return new CapturingExecutor(TableMetadata.builder()
            .name("person").columnsMetadata(List.of(age)).build());
    }

    @Test
    void addAndDropCheckConstraint_supported() throws Exception {
        CapturingExecutor executor = checkExecutor();

        executor.addColumnConstraint(ConstraintType.CHECK, "age");
        assertTrue(executor.capturedSql.contains(
            "ADD CONSTRAINT constraint_age CHECK age > 0"), executor.capturedSql);

        executor.dropColumnConstraint(ConstraintType.CHECK, "constraint_age");
        assertTrue(executor.capturedSql.contains("DROP CONSTRAINT constraint_age"), executor.capturedSql);
    }

    @Test
    void nonCheckConstraints_unsupported() {
        CapturingExecutor executor = checkExecutor();
        assertThrows(UnsupportedOperationException.class,
            () -> executor.addColumnConstraint(ConstraintType.PRIMARY_KEY, "age"));
        assertThrows(UnsupportedOperationException.class,
            () -> executor.dropColumnConstraint(ConstraintType.FOREIGN_KEY, "x"));
    }
}
