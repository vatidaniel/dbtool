package io.github.vatidaniel.metadata.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the typed {@link DataType} model on {@link ColumnMetadata}: the rendered SQL fragment and the
 * {@link DataType#of(String)} raw escape hatch for types without a dialect enum constant.
 *
 * @author tinhnv
 * @since Jun 14, 2026
 */
class DataTypeTest {

    @Test
    void rawDataType_rendersKeywordAndReportsUnknownCategory() {
        DataType raw = DataType.of("VECTOR");
        assertEquals("VECTOR", raw.getKeyWord());
        assertEquals("VECTOR", raw.getValue());
        assertEquals("VECTOR", raw.getLabel());
        // unknown category so category-driven logic falls back to the old raw-String behaviour
        assertNull(raw.getParent());
        assertTrue(raw.isEnable());
    }

    @Test
    void rawDataType_equalsByKeyword() {
        assertEquals(DataType.of("VECTOR"), DataType.of("VECTOR"));
        assertEquals(DataType.of("VECTOR").hashCode(), DataType.of("VECTOR").hashCode());
        assertFalse(DataType.of("VECTOR").equals(DataType.of("JSONB")));
    }

    @Test
    void rawDataType_blankKeyword_throws() {
        assertThrows(IllegalArgumentException.class, () -> DataType.of(null));
        assertThrows(IllegalArgumentException.class, () -> DataType.of("   "));
    }

    @Test
    void getDataTypeDefinition_appendsExtensionWhenPresent() {
        ColumnMetadata withExt = ColumnMetadata.builder()
            .name("embedding").dataType(DataType.of("VECTOR")).dataTypeExtension("768").build();
        assertEquals("VECTOR(768)", withExt.getDataTypeDefinition());

        ColumnMetadata noExt = ColumnMetadata.builder()
            .name("id").dataType(DataType.of("VECTOR")).build();
        assertEquals("VECTOR", noExt.getDataTypeDefinition());
    }

    @Test
    void getDataTypeDefinition_nullDataType_returnsNull() {
        ColumnMetadata bare = ColumnMetadata.builder().name("x").build();
        assertNull(bare.getDataTypeDefinition());
    }
}
