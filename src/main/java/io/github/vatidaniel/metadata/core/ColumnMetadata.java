package io.github.vatidaniel.metadata.core;

import io.github.vatidaniel.dataaccess.SqlQueryConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author tinhnv
 * @since Dec 20, 2023
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ColumnMetadata {

    protected String name;
    protected DataType dataType;
    protected String dataTypeExtension;
    protected boolean identity;
    /**
     * There are many cases occur when change primary key constraint
     * @see <a href='https://stackoverflow.com/questions/2111291/remove-primary-key-in-mysql'>Primary key update issue</a>
     */
    protected boolean primaryKey;
    @Builder.Default
    protected boolean nullable = true;
    protected boolean unique;
    protected ReferenceMetadata referenceMetadata;
    protected DefaultColumnValue columnDefault;
    protected String checkConstraint;

    public ColumnMetadata(ColumnMetadata other) {
        this.name = other.name;
        this.dataType = other.dataType;
        this.dataTypeExtension = other.dataTypeExtension;
        this.identity = other.identity;
        this.primaryKey = other.primaryKey;
        this.nullable = other.nullable;
        this.unique = other.unique;
        this.checkConstraint = other.checkConstraint;
        this.referenceMetadata = other.referenceMetadata != null ? new ReferenceMetadata(other.referenceMetadata) : null;
        this.columnDefault = other.columnDefault != null ? new DefaultColumnValue(other.columnDefault) : null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DefaultColumnValue {

        private DataType dataType;
        private Object value;

        public DefaultColumnValue(DefaultColumnValue other) {
            this.dataType = other.dataType;
            this.value = other.value;
        }
    }

    /**
     * The full column type as it appears in SQL: the {@link #dataType} keyword plus the optional
     * {@link #dataTypeExtension} (e.g. {@code VARCHAR} + {@code 255} -&gt; {@code VARCHAR(255)}).
     * Use this when emitting DDL; {@link #getDataType()} returns the typed {@link DataType} itself.
     */
    public String getDataTypeDefinition() {
        if (dataType == null) return null;
        if (dataTypeExtension == null || dataTypeExtension.isBlank()) return dataType.getKeyWord();
        return dataType.getKeyWord() + SqlQueryConstants.roundBracketWrap(dataTypeExtension);
    }

}
