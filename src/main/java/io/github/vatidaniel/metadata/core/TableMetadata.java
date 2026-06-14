package io.github.vatidaniel.metadata.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author tinhnv
 * @since Dec 20, 2023
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableMetadata {

    private String name;
    private List<ColumnMetadata> columnsMetadata;
    private String tablespace;

    public TableMetadata(TableMetadata other) {
        this.name = other.name;
        this.tablespace = other.tablespace;
        List<ColumnMetadata> clonedColumnsMetadata = new ArrayList<>();
        other.columnsMetadata.stream().filter(Objects::nonNull).map(ColumnMetadata::new)
            .forEach(clonedColumnsMetadata::add);
        this.columnsMetadata = clonedColumnsMetadata;
    }
}
