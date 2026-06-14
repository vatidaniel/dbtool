package io.github.vatidaniel.metadata.sqlserver;

import io.github.vatidaniel.metadata.core.ColumnMetadata;
import io.github.vatidaniel.metadata.core.ConstraintType;
import io.github.vatidaniel.metadata.core.TableMetadata;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author tinhnv
 */
class SqlServerDdlExecutorTest {

    private static class CapturingExecutor extends SqlServerDdlExecutor {
        private String capturedSql;

        CapturingExecutor(TableMetadata tableMetadata) {
            super(tableMetadata, (Connection) null);
        }

        @Override
        public void executeSql(String sql) {
            this.capturedSql = sql;
        }
    }

    private TableMetadata personTable() {
        return TableMetadata.builder().name("person").columnsMetadata(List.of(
            ColumnMetadata.builder().name("id").dataType(SqlServerDataType.INT).primaryKey(true).nullable(false).identity(true).build(),
            ColumnMetadata.builder().name("name").dataType(SqlServerDataType.VARCHAR).dataTypeExtension("255").build()
        )).build();
    }

    @Test
    void createTable_usesBracketQuotingAndIdentity() throws Exception {
        CapturingExecutor e = new CapturingExecutor(personTable());
        e.createTable();
        String sql = e.capturedSql;
        assertNotNull(sql);
        assertTrue(sql.contains("CREATE TABLE [person]"), sql);
        assertTrue(sql.contains("[id] INT"), sql);
        assertTrue(sql.contains("IDENTITY(1,1)"), sql);
        assertTrue(sql.contains("[name] VARCHAR(255)"), sql);
        assertTrue(sql.contains("PRIMARY KEY([id])"), sql);
    }

    @Test
    void addColumn_usesAddWithoutColumnKeyword() throws Exception {
        CapturingExecutor e = new CapturingExecutor(personTable());
        e.addColumn("name");
        assertEquals("ALTER TABLE [person] ADD [name] VARCHAR(255)", e.capturedSql);
    }

    @Test
    void updateColumnDefinition_usesAlterColumn() throws Exception {
        CapturingExecutor e = new CapturingExecutor(personTable());
        e.updateColumnDefinition("name");
        assertEquals("ALTER TABLE [person] ALTER COLUMN [name] VARCHAR(255)", e.capturedSql);
    }

    @Test
    void renameTableAndColumn_useSpRename() throws Exception {
        CapturingExecutor e = new CapturingExecutor(personTable());
        e.renameColumn("name", "full_name");
        assertEquals("EXEC sp_rename 'person.name', 'full_name', 'COLUMN'", e.capturedSql);
        e.renameTable("people");
        assertEquals("EXEC sp_rename 'person', 'people'", e.capturedSql);
    }

    @Test
    void renameColumn_escapesSingleQuotesInLiterals() throws Exception {
        TableMetadata table = TableMetadata.builder().name("per'son").columnsMetadata(List.of(
            ColumnMetadata.builder().name("na'me").dataType(SqlServerDataType.INT).build()
        )).build();
        CapturingExecutor e = new CapturingExecutor(table);
        // embedded quotes are doubled so they cannot break out of the sp_rename string literals
        e.renameColumn("na'me", "ful'l");
        assertEquals("EXEC sp_rename 'per''son.na''me', 'ful''l', 'COLUMN'", e.capturedSql);
        e.renameTable("peo'ple");
        assertEquals("EXEC sp_rename 'per''son', 'peo''ple'", e.capturedSql);
    }

    @Test
    void constraints_useSqlServerForms() throws Exception {
        CapturingExecutor e = new CapturingExecutor(personTable());

        e.addColumnConstraint(ConstraintType.NOT_NULL, "name");
        assertEquals("ALTER TABLE [person] ALTER COLUMN [name] VARCHAR(255) NOT NULL", e.capturedSql);

        e.addColumnConstraint(ConstraintType.PRIMARY_KEY, "id");
        assertEquals("ALTER TABLE [person] ADD PRIMARY KEY ([id])", e.capturedSql);

        e.dropColumnConstraint(ConstraintType.FOREIGN_KEY, "fk_person");
        assertEquals("ALTER TABLE [person] DROP CONSTRAINT fk_person", e.capturedSql);

        e.dropColumnConstraint(ConstraintType.NOT_NULL, "name");
        assertEquals("ALTER TABLE [person] ALTER COLUMN [name] VARCHAR(255) NULL", e.capturedSql);
    }
}
