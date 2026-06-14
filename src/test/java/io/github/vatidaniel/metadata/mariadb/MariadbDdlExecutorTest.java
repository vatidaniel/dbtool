package io.github.vatidaniel.metadata.mariadb;

import io.github.vatidaniel.metadata.core.ColumnMetadata;
import io.github.vatidaniel.metadata.core.ConstraintType;
import io.github.vatidaniel.metadata.core.ReferenceActionType;
import io.github.vatidaniel.metadata.core.ReferenceMetadata;
import io.github.vatidaniel.metadata.core.TableMetadata;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
            .dataType(MariadbDataType.INT)
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
            .name("id").dataType(MariadbDataType.INT).primaryKey(true).nullable(false).identity(true)
            .build();
        ColumnMetadata name = ColumnMetadata.builder()
            .name("name").dataType(MariadbDataType.VARCHAR).dataTypeExtension("255")
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
    void createTable_escapesEmbeddedQuoteInStringDefault() throws Exception {
        ColumnMetadata name = ColumnMetadata.builder()
            .name("name").dataType(MariadbDataType.VARCHAR).dataTypeExtension("255")
            .columnDefault(ColumnMetadata.DefaultColumnValue.builder()
                .dataType(MariadbDataType.VARCHAR).value("O'Brien").build())
            .build();
        TableMetadata table = TableMetadata.builder()
            .name("t").columnsMetadata(List.of(name)).build();

        CapturingExecutor e = new CapturingExecutor(table);
        e.createTable();

        // the embedded single quote is doubled, not left to terminate the literal early
        assertTrue(e.capturedSql.contains("DEFAULT 'O''Brien'"), e.capturedSql);
    }

    @Test
    void createTable_withNoColumns_throws() {
        TableMetadata table = TableMetadata.builder()
            .name("empty")
            .columnsMetadata(Collections.emptyList())
            .build();
        CapturingExecutor executor = new CapturingExecutor(table);
        assertThrows(IllegalStateException.class, executor::createTable);
    }

    @Test
    void createTable_quotesTemporalDefaultAndRendersNullDefault() throws Exception {
        ColumnMetadata created = ColumnMetadata.builder()
            .name("created").dataType(MariadbDataType.DATETIME)
            .columnDefault(ColumnMetadata.DefaultColumnValue.builder()
                .dataType(MariadbDataType.DATETIME).value("2024-01-01 00:00:00").build())
            .build();
        ColumnMetadata note = ColumnMetadata.builder()
            .name("note").dataType(MariadbDataType.VARCHAR).dataTypeExtension("50")
            .columnDefault(ColumnMetadata.DefaultColumnValue.builder()
                .dataType(MariadbDataType.VARCHAR).value(null).build())
            .build();
        TableMetadata table = TableMetadata.builder()
            .name("t").columnsMetadata(List.of(created, note)).build();

        CapturingExecutor e = new CapturingExecutor(table);
        e.createTable();

        assertTrue(e.capturedSql.contains("DEFAULT '2024-01-01 00:00:00'"), e.capturedSql);
        assertTrue(e.capturedSql.contains("DEFAULT NULL"), e.capturedSql);
    }

    private CapturingExecutor constraintExecutor() {
        return new CapturingExecutor(TableMetadata.builder().name("person").columnsMetadata(List.of(
            ColumnMetadata.builder().name("id").dataType(MariadbDataType.INT).build(),
            ColumnMetadata.builder().name("age").dataType(MariadbDataType.INT).checkConstraint("> 0").build(),
            ColumnMetadata.builder().name("email").dataType(MariadbDataType.VARCHAR).dataTypeExtension("255").build(),
            ColumnMetadata.builder().name("parent_id").dataType(MariadbDataType.INT)
                .referenceMetadata(new ReferenceMetadata("parent", "id",
                    ReferenceActionType.CASCADE, ReferenceActionType.CASCADE)).build()
        )).build());
    }

    @Test
    void addColumnConstraint_emitsMariadbForms() throws Exception {
        CapturingExecutor e = constraintExecutor();

        e.addColumnConstraint(ConstraintType.PRIMARY_KEY, "id");
        assertEquals("ALTER TABLE `person` ADD PRIMARY KEY (`id`)", e.capturedSql);

        e.addColumnConstraint(ConstraintType.UNIQUE, "email");
        assertEquals("ALTER TABLE `person` ADD CONSTRAINT `person_email_uq` UNIQUE (`email`)", e.capturedSql);

        e.addColumnConstraint(ConstraintType.CHECK, "age");
        assertEquals("ALTER TABLE `person` ADD CONSTRAINT `person_age_chk` CHECK (`age` > 0)", e.capturedSql);

        e.addColumnConstraint(ConstraintType.NOT_NULL, "id");
        assertEquals("ALTER TABLE `person` MODIFY `id` INT NOT NULL", e.capturedSql);

        e.addColumnConstraint(ConstraintType.FOREIGN_KEY, "parent_id");
        assertEquals("ALTER TABLE `person` ADD CONSTRAINT `person_parent_id_fk` "
            + "FOREIGN KEY (`parent_id`) REFERENCES `parent`(`id`) ON DELETE CASCADE ON UPDATE CASCADE", e.capturedSql);
    }

    @Test
    void dropColumnConstraint_emitsMariadbForms() throws Exception {
        CapturingExecutor e = constraintExecutor();

        e.dropColumnConstraint(ConstraintType.PRIMARY_KEY, "ignored");
        assertEquals("ALTER TABLE `person` DROP PRIMARY KEY", e.capturedSql);

        e.dropColumnConstraint(ConstraintType.UNIQUE, "uq_email");
        assertEquals("ALTER TABLE `person` DROP INDEX uq_email", e.capturedSql);

        e.dropColumnConstraint(ConstraintType.CHECK, "ck_age");
        assertEquals("ALTER TABLE `person` DROP CHECK ck_age", e.capturedSql);

        e.dropColumnConstraint(ConstraintType.FOREIGN_KEY, "fk_parent");
        assertEquals("ALTER TABLE `person` DROP FOREIGN KEY fk_parent", e.capturedSql);

        e.dropColumnConstraint(ConstraintType.NOT_NULL, "id");
        assertEquals("ALTER TABLE `person` MODIFY `id` INT", e.capturedSql);
    }

    @Test
    void columnOperations_quoteTableAndColumnIdentifiers() throws Exception {
        CapturingExecutor e = constraintExecutor();

        e.dropColumn("email");
        assertEquals("ALTER TABLE `person` DROP COLUMN `email`", e.capturedSql);

        e.renameColumn("email", "mail");
        assertEquals("ALTER TABLE `person` RENAME COLUMN `email` TO `mail`", e.capturedSql);

        e.dropTable();
        assertEquals("DROP TABLE `person`", e.capturedSql);
    }
}
