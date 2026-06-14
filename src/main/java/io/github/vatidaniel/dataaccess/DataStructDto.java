package io.github.vatidaniel.dataaccess;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Sample data struct with a header list
 *
 * @author tinhnv
 * @since Oct 31, 2023
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataStructDto {

    private Queue<RowStruct> columns;
    private List<Map<Integer, String>> values;
    private Pagination pagination;

}
