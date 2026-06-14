package io.github.vatidaniel.metadata.postgres;

import io.github.vatidaniel.metadata.core.DataType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author tinhnv
 * @since Jun 13, 2026
 *
 * @see <a href='https://www.postgresql.org/docs/current/datatype.html'>PostgreSQL data types</a>
 */
@Getter
@RequiredArgsConstructor
public enum PostgresDataType implements DataType {

    SMALLINT("SMALLINT", DataType.BasicDataType.NUMERIC, "2-byte signed integer, range -32768 to 32767"),
    INTEGER("INTEGER", DataType.BasicDataType.NUMERIC, "4-byte signed integer"),
    BIGINT("BIGINT", DataType.BasicDataType.NUMERIC, "8-byte signed integer"),
    NUMERIC("NUMERIC", DataType.BasicDataType.NUMERIC, "Exact numeric of selectable precision - NUMERIC(p, s)"),
    REAL("REAL", DataType.BasicDataType.NUMERIC, "4-byte single precision floating-point number"),
    DOUBLE_PRECISION("DOUBLE PRECISION", DataType.BasicDataType.NUMERIC, "8-byte double precision floating-point number"),
    BOOLEAN("BOOLEAN", DataType.BasicDataType.NUMERIC, "Logical Boolean (true/false)"),

    VARCHAR("VARCHAR", DataType.BasicDataType.STRING, "Variable-length character string - VARCHAR(n)"),
    CHAR("CHAR", DataType.BasicDataType.STRING, "Fixed-length, blank-padded character string - CHAR(n)"),
    TEXT("TEXT", DataType.BasicDataType.STRING, "Variable unlimited length character string"),
    BYTEA("BYTEA", DataType.BasicDataType.STRING, "Binary data (\"byte array\")"),
    UUID("UUID", DataType.BasicDataType.STRING, "Universally unique identifier"),
    JSON("JSON", DataType.BasicDataType.STRING, "Textual JSON data"),
    JSONB("JSONB", DataType.BasicDataType.STRING, "Binary JSON data, decomposed"),

    DATE("DATE", DataType.BasicDataType.TEMPORAL, "Calendar date (year, month, day)"),
    TIME("TIME", DataType.BasicDataType.TEMPORAL, "Time of day (no time zone)"),
    TIMESTAMP("TIMESTAMP", DataType.BasicDataType.TEMPORAL, "Date and time (no time zone)"),
    TIMESTAMPTZ("TIMESTAMP WITH TIME ZONE", DataType.BasicDataType.TEMPORAL, "Date and time, including time zone"),
    INTERVAL("INTERVAL", DataType.BasicDataType.TEMPORAL, "Time span"),

    POINT("POINT", DataType.BasicDataType.SPATIAL, "Geometric point on a plane - (x, y)"),
    LINE("LINE", DataType.BasicDataType.SPATIAL, "Infinite geometric line"),
    POLYGON("POLYGON", DataType.BasicDataType.SPATIAL, "Closed geometric path on a plane"),
    GEOMETRY("GEOMETRY", DataType.BasicDataType.SPATIAL, "PostGIS spatial geometry"),

    ;

    private final String keyWord; // fix the enum name()
    private final DataType parent;
    private final String description;
    private final boolean enable = true;

}
