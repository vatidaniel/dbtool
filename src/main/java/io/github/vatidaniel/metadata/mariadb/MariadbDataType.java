package io.github.vatidaniel.metadata.mariadb;

import io.github.vatidaniel.metadata.core.DataType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static io.github.vatidaniel.metadata.core.DataType.BasicDataType.*;

/**
 * @author tinhnv
 * @since Dec 19, 2023
 */
@Getter
@RequiredArgsConstructor
public enum MariadbDataType implements DataType {

    TINYINT(NUMERIC, "Very small integer"),
    MEDIUMINT(NUMERIC, "Medium-sized integer"),
    INT(NUMERIC, "Standard integer"),
    BIGINT(NUMERIC, "Large integer"),
    DECIMAL(NUMERIC, "Fixed-point number"),
    FLOAT(NUMERIC, "Single-precision floating-point number"),
    DOUBLE(NUMERIC, "Double-precision floating-point number"),
    BIT(NUMERIC, "A bit"),

    CHAR(STRING, "Fixed-length non-binary (character) string"),
    VARCHAR(STRING, "Variable-length non-binary string"),
    BINARY(STRING, "Fixed-length binary string"),
    VARBINARY(STRING, "Variable-length binary string"),
    TINYBLOB(STRING, "Very small BLOB (binary large object)"),
    BLOB(STRING, "Small BLOB"),
    MEDIUMBLOB(STRING, "Medium-sized BLOB"),
    LONGBLOB(STRING, "Large BLOB"),
    TINYTEXT(STRING, "Very small non-binary string"),
    TEXT(STRING, "Small non-binary string"),
    MEDIUMTEXT(STRING, "Medium-sized non-binary string"),
    LONGTEXT(STRING, "Large non-binary string"),
    ENUM(STRING, "Enumeration"),
    SET(STRING, "A set"),

    DATE(TEMPORAL, "Date value in CCYY-MM-DD format"),
    TIME(TEMPORAL, "Time value in hh:mm:ss format"),
    DATETIME(TEMPORAL, "Date and time value in CCYY-MM-DD hh:mm:ss format"),
    TIMESTAMP(TEMPORAL, "Timestamp value in CCYY-MM-DD hh:mm:ss format"),
    YEAR(TEMPORAL, "Year value in CCYY or YY format"),

    GEOMETRY(SPATIAL, "Spatial value of any type"),
    POINT(SPATIAL, "Point (a pair of X-Y coordinates)"),
    LINESTRING(SPATIAL, "Curve (one or more POINT values)"),
    POLYGON(SPATIAL, "Polygon"),
    GEOMETRYCOLLECTION(SPATIAL, "Collection of GEOMETRY values"),
    MULTILINESTRING(SPATIAL, "Collection of LINESTRING values"),
    MULTIPOINT(SPATIAL, "Collection of POINT values"),
    MULTIPOLYGON(SPATIAL, "Collection of POLYGON values"),

    ;

    private final DataType parent;
    private final String description;
    private final boolean enable = true;

}
