package io.github.vatidaniel.dataaccess;

import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * @author tinhnv
 * @since Oct 31, 2023
 */
public interface ExecuteDataService {

    /**
     * Get columns from a specific ds table
     *
     * @param dsTableId the synchronized data standardization table id
     * @return column list
     */
    Queue<RowStruct> getColumns(Long dsTableId);

    /**
     * Fetch ds table data with a column struct
     *
     * @param dsTableId the synchronized data standardization table id
     * @param versionId the version id (Mar 13, 2024 - This is the data-flow-instance id for each time ETL)
     * @param page page index
     * @param size number of row
     * @return data for each row
     */
    List<List<RowStruct>> getValues(Long dsTableId, Long versionId, int page, int size);

    /**
     * Fetch ds table data with a column struct as a map, the map key is columns index
     *
     * @param dsTableId the synchronized data standardization table id
     * @param versionId the version id (Mar 13, 2024 - This is the data-flow-instance id for each time ETL)
     * @param page page index
     * @param size number of rows
     * @return data as a map for each row
     */
    List<Map<Integer, String>> getValuesAsMap(Long dsTableId, Long versionId, int page, int size);

    /**
     * Count total ds table row
     *
     * @param dsTableId the synchronized data standardization table id
     * @return total ds table row
     */
    long totalRowCount(Long dsTableId);

    /**
     * Fetch ds table metadata with a column list, value and total existing value
     *
     * @param dsTableId the synchronized data standardization id
     * @param versionId the data version id
     * @param page      page index
     * @param size      number of rows
     * @return {@link DataStructDto}
     */
    DataStructDto fetchData(Long dsTableId, Long versionId, int page, int size);

}
