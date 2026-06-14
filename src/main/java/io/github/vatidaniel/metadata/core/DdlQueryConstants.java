package io.github.vatidaniel.metadata.core;

import io.github.vatidaniel.dataaccess.SqlQueryConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author tinhnv
 * @since Dec 20, 2023
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DdlQueryConstants extends SqlQueryConstants {

    protected static final String ALTER_TABLE = "ALTER TABLE ";
    protected static final String ALTER_COLUMN = " ALTER COLUMN ";

    protected static final String PRIMARY_KEY = "PRIMARY KEY";
    protected static final String FOREIGN_KEY = "FOREIGN KEY";
    protected static final String REFERENCES = "REFERENCES";

}
