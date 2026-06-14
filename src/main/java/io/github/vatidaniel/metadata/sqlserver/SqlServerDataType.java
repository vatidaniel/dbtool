package io.github.vatidaniel.metadata.sqlserver;

import io.github.vatidaniel.metadata.core.DataType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author tinhnv
 * @since Jun 13, 2026
 *
 * @see <a href='https://learn.microsoft.com/en-us/sql/t-sql/data-types/data-types-transact-sql'>SQL Server data types</a>
 */
@Getter
@RequiredArgsConstructor
public enum SqlServerDataType implements DataType {

    TINYINT("TINYINT", DataType.BasicDataType.NUMERIC, "1-byte unsigned integer, 0 to 255"),
    SMALLINT("SMALLINT", DataType.BasicDataType.NUMERIC, "2-byte signed integer"),
    INT("INT", DataType.BasicDataType.NUMERIC, "4-byte signed integer"),
    BIGINT("BIGINT", DataType.BasicDataType.NUMERIC, "8-byte signed integer"),
    BIT("BIT", DataType.BasicDataType.NUMERIC, "Integer 0/1/NULL (boolean)"),
    DECIMAL("DECIMAL", DataType.BasicDataType.NUMERIC, "Fixed precision/scale - DECIMAL(p, s)"),
    NUMERIC("NUMERIC", DataType.BasicDataType.NUMERIC, "Synonym for DECIMAL"),
    MONEY("MONEY", DataType.BasicDataType.NUMERIC, "Currency value"),
    FLOAT("FLOAT", DataType.BasicDataType.NUMERIC, "Approximate floating-point number"),
    REAL("REAL", DataType.BasicDataType.NUMERIC, "4-byte approximate floating-point number"),

    CHAR("CHAR", DataType.BasicDataType.STRING, "Fixed-length non-Unicode string - CHAR(n)"),
    VARCHAR("VARCHAR", DataType.BasicDataType.STRING, "Variable-length non-Unicode string - VARCHAR(n)"),
    NCHAR("NCHAR", DataType.BasicDataType.STRING, "Fixed-length Unicode string - NCHAR(n)"),
    NVARCHAR("NVARCHAR", DataType.BasicDataType.STRING, "Variable-length Unicode string - NVARCHAR(n)"),
    TEXT("TEXT", DataType.BasicDataType.STRING, "Variable-length non-Unicode large string (deprecated)"),
    BINARY("BINARY", DataType.BasicDataType.STRING, "Fixed-length binary data - BINARY(n)"),
    VARBINARY("VARBINARY", DataType.BasicDataType.STRING, "Variable-length binary data - VARBINARY(n)"),
    UNIQUEIDENTIFIER("UNIQUEIDENTIFIER", DataType.BasicDataType.STRING, "16-byte GUID"),

    DATE("DATE", DataType.BasicDataType.TEMPORAL, "Calendar date"),
    TIME("TIME", DataType.BasicDataType.TEMPORAL, "Time of day"),
    DATETIME("DATETIME", DataType.BasicDataType.TEMPORAL, "Date and time"),
    DATETIME2("DATETIME2", DataType.BasicDataType.TEMPORAL, "Date and time with larger range/precision"),
    SMALLDATETIME("SMALLDATETIME", DataType.BasicDataType.TEMPORAL, "Date and time, minute precision"),
    DATETIMEOFFSET("DATETIMEOFFSET", DataType.BasicDataType.TEMPORAL, "Date and time with time-zone offset"),

    GEOMETRY("GEOMETRY", DataType.BasicDataType.SPATIAL, "Planar spatial data"),
    GEOGRAPHY("GEOGRAPHY", DataType.BasicDataType.SPATIAL, "Ellipsoidal (round-earth) spatial data"),

    ;

    private final String keyWord; // fix the enum name()
    private final DataType parent;
    private final String description;
    private final boolean enable = true;

}
