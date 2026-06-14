package io.github.vatidaniel.metadata.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author tinhnv
 * @since Dec 20, 2023
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceMetadata {

    private String tableName;
    private String columnName;
    private ReferenceActionType onDelete;
    private ReferenceActionType onUpdate;

    public ReferenceMetadata(ReferenceMetadata other) {
        this.tableName = other.tableName;
        this.columnName = other.columnName;
        this.onDelete = other.onDelete;
        this.onUpdate = other.onUpdate;
    }

}
