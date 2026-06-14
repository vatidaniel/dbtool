package io.github.vatidaniel.metadata.clickhouse;

import io.github.vatidaniel.metadata.core.DataType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static io.github.vatidaniel.metadata.core.DataType.BasicDataType.*;

/**
 * @author tinhnv
 * @since Mar 31, 2024
 *
 * @see <a href='https://clickhouse.com/docs/en/sql-reference/data-types'>ClickHouse data types</a>
 */
@Getter
@RequiredArgsConstructor
public enum ClickHouseDataType implements DataType {

    INT_8("Int8", NUMERIC, "[-128 : 127] - TINYINT, INT1, BYTE, TINYINT SIGNED, INT1 SIGNED"),
    INT_16("Int16", NUMERIC, "[-32768 : 32767] - SMALLINT, SMALLINT SIGNED"),
    INT_32("Int32", NUMERIC, "[-2147483648 : 2147483647] - INT, INTEGER, MEDIUMINT, MEDIUMINT SIGNED, INT SIGNED, INTEGER SIGNED"),
    INT_64("Int64", NUMERIC, "[-9223372036854775808 : 9223372036854775807] - BIGINT, SIGNED, BIGINT SIGNED, TIME"),
    INT_128("Int128", NUMERIC, "[-170141183460469231731687303715884105728 : 170141183460469231731687303715884105727]"),
    INT_256("Int256", NUMERIC, "[-57896044618658097711785492504343953926634992332820282019728792003956564819968 : 57896044618658097711785492504343953926634992332820282019728792003956564819967]"),
    U_INT_8("UInt8", NUMERIC, "[0 : 255] - TINYINT UNSIGNED, INT1 UNSIGNED"),
    U_INT_16("UInt16", NUMERIC, "[0 : 65535] - SMALLINT UNSIGNED"),
    U_INT_32("UInt32", NUMERIC, "[0 : 4294967295] - MEDIUMINT UNSIGNED, INT UNSIGNED, INTEGER UNSIGNED"),
    U_INT_64("UInt64", NUMERIC, "[0 : 18446744073709551615] - UNSIGNED, BIGINT UNSIGNED, BIT, SET"),
    U_INT_128("UInt128", NUMERIC, "[0 : 340282366920938463463374607431768211455]"),
    U_INT_256("UInt256", NUMERIC, "[0 : 115792089237316195423570985008687907853269984665640564039457584007913129639935]"),
    FLOAT_32("Float32", NUMERIC, "FLOAT, REAL, SINGLE"),
    FLOAT_64("Float64", NUMERIC, "DOUBLE, DOUBLE PRECISION"),
    DECIMAL("Decimal", NUMERIC, "Decimal(P,S): Decimal(P) is equivalent to Decimal(P, 0). Similarly, the syntax Decimal is equivalent to Decimal(10, 0)."),
    DECIMAL_32("Decimal32", NUMERIC, "P[1:9] - Decimal32(S) - ( -1 * 10^(9 - S), 1 * 10^(9 - S) )"),
    DECIMAL_64("Decimal64", NUMERIC, "P[10:18] - Decimal64(S) - ( -1 * 10^(18 - S), 1 * 10^(18 - S) )"),
    DECIMAL_128("Decimal128", NUMERIC, "P[19:38] - Decimal128(S) - ( -1 * 10^(38 - S), 1 * 10^(38 - S) )"),
    DECIMAL_256("Decimal256", NUMERIC, "P[39:76] - Decimal256(S) - ( -1 * 10^(76 - S), 1 * 10^(76 - S) )"),
    BOOL("Bool", NUMERIC, "Type bool is internally stored as UInt8. Possible values are true (1), false (0)"),

    CH_STRING("String", STRING, "LONGTEXT, MEDIUMTEXT, TINYTEXT, TEXT, LONGBLOB, MEDIUMBLOB, TINYBLOB, BLOB, VARCHAR, CHAR"),
    FIXED_STRING("FixedString", STRING, "FixedString(N) - A fixed-length string of N bytes (neither characters nor code points)"),
    UUID("UUID", STRING, "A Universally Unique Identifier (UUID) is a 16-byte value used to identify records"),
    JSON("JSON", STRING, "") {
        // The JSON data type is an obsolete feature. Do not use it. If you want to use it, set allow_experimental_object_type = 1.
        @Override
        public boolean isEnable() { return false; }
    },
    ENUM("ENUM", STRING, "Enumerated type consisting of named values."),

    DATE("DATE", TEMPORAL, "[1970-01-01, 2149-06-06] The date value is stored without the time zone"),
    DATE_32("DATE32", TEMPORAL, "Stored as a signed 32-bit integer in native byte order with the value representing the days since 1970-01-01 (0 represents 1970-01-01 and negative values represent the days before 1970)."),
    DATETIME("DATETIME", TEMPORAL, "[1970-01-01 00:00:00, 2106-02-07 06:28:15] DateTime([timezone])"),
    DATETIME_64("DATETIME64", TEMPORAL, "[1900-01-01 00:00:00, 2299-12-31 23:59:59.99999999] DateTime64(precision, [timezone])"),

    LOW_CARDINALITY("LowCardinality", null, "LowCardinality(data_type) - Changes the internal representation of other data types to be dictionary-encoded"),
    ARRAY("Array", null, "An array of T-type items, with the starting array index as 1. T can be any data type, including an array."),
    MAP("Map", null, "Map(key, value) data type stores key:value pairs"),
    SIMPLE_AGGREGATE_FUNCTION("SimpleAggregateFunction", null, "SimpleAggregateFunction(function_name, types_of_arguments…)"),
    AGGREGATE_FUNCTION("AggregateFunction", null, "AggregateFunction(function_name, types_of_arguments…)"),
    NESTED("Nested", null, "Nested(name1 Type1, Name2 Type2, …)"),
    TUPLE("Tuple", null, "A tuple of elements, each having an individual type. Tuple must contain at least one element."),

    IP_V4("IPv4", STRING, "IPv4 addresses. Stored in 4 bytes as UInt32."),
    IP_V6("IPv6", STRING, "IPv6 addresses. Stored in 16 bytes as UInt128 big-endian"),

    POINT("Point", SPATIAL, "Point is represented by its X and Y coordinates, stored as a Tuple(Float64, Float64)"),
    RING("Ring", SPATIAL, "Ring is a simple polygon without holes stored as an array of points: Array(Point)"),
    POLYGON("Polygon", SPATIAL, "Polygon is a polygon with holes stored as an array of rings: Array(Ring). First element of outer array is the outer shape of polygon and all the following elements are holes"),
    MULTI_POLYGON("MultiPolygon", SPATIAL, "MultiPolygon consists of multiple polygons and is stored as an array of polygons: Array(Polygon)"),

    ;

    private final String keyWord; // fix the enum name()
    private final DataType parent;
    private final String description;
    private final boolean enable = true;
}
