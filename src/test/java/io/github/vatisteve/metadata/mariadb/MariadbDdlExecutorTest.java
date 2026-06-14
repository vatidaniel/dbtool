package io.github.vatisteve.metadata.mariadb;

import io.github.vatisteve.metadata.core.ColumnMetadata;
import io.github.vatisteve.metadata.core.ReferenceActionType;
import io.github.vatisteve.metadata.core.ReferenceMetadata;
import io.github.vatisteve.metadata.core.TableMetadata;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author tinhnv
 */
class MariadbDdlExecutorTest {

    /**
     * Captures the generated SQL instead of running it against a JDBC connection,
     * so DDL generation can be asserted without a real database.
     */
    private static class CapturingExecutor extends MariadbDdlExecutor {
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
    void createTable_foreignKey_usesDistinctOnDeleteAndOnUpdate() throws Exception {
        // Regression for the bug where ON UPDATE re-used the ON DELETE action.
        // Referenced names are passed raw; the executor quotes them via the dialect.
        ReferenceMetadata ref = new ReferenceMetadata("parent", "id",
            ReferenceActionType.CASCADE, ReferenceActionType.SET_NULL);
        ColumnMetadata fkColumn = ColumnMetadata.builder()
            .name("parent_id")
            .dataType("INT")
            .referenceMetadata(ref)
            .build();
        TableMetadata table = TableMetadata.builder()
            .name("child")
            .columnsMetadata(List.of(fkColumn))
            .build();

        CapturingExecutor executor = new CapturingExecutor(table);
        executor.createTable();
        String sql = executor.capturedSql;

        assertNotNull(sql);
        assertTrue(sql.contains("FOREIGN KEY (`parent_id`) REFERENCES `parent`(`id`)"), sql);
        assertTrue(sql.contains("ON DELETE CASCADE"), sql);
        assertTrue(sql.contains("ON UPDATE SET NULL"), sql);
    }

    @Test
    void createTable_emitsColumnPrimaryKeyAndTypedDefault() throws Exception {
        ColumnMetadata id = ColumnMetadata.builder()
            .name("id").dataType("INT").primaryKey(true).nullable(false).identity(true)
            .build();
        ColumnMetadata name = ColumnMetadata.builder()
            .name("name").dataType("VARCHAR").dataTypeExtension("255")
            .columnDefault(ColumnMetadata.DefaultColumnValue.builder()
                .dataType(MariadbDataType.VARCHAR).value("unknown").build())
            .build();
        TableMetadata table = TableMetadata.builder()
            .name("person")
            .columnsMetadata(List.of(id, name))
            .build();

        CapturingExecutor executor = new CapturingExecutor(table);
        executor.createTable();
        String sql = executor.capturedSql;

        assertNotNull(sql);
        assertTrue(sql.contains("CREATE TABLE `person`"), sql);
        assertTrue(sql.contains("`id` INT"), sql);
        assertTrue(sql.contains("AUTO_INCREMENT"), sql);
        assertTrue(sql.contains("`name` VARCHAR(255)"), sql);
        assertTrue(sql.contains("DEFAULT 'unknown'"), sql);
        assertTrue(sql.contains("PRIMARY KEY(`id`)"), sql);
    }

    @Test
    void createTable_withNoColumns_doesNotThrow() throws Exception {
        TableMetadata table = TableMetadata.builder()
            .name("empty")
            .columnsMetadata(Collections.emptyList())
            .build();
        CapturingExecutor executor = new CapturingExecutor(table);
        executor.createTable();
        assertTrue(executor.capturedSql.contains("CREATE TABLE `empty`"), executor.capturedSql);
    }
}
