package io.github.vatidaniel.metadata.mariadb;

import io.github.vatidaniel.dataaccess.MariadbDialect;
import io.github.vatidaniel.metadata.core.StandardSqlDdlExecutor;
import io.github.vatidaniel.metadata.core.TableMetadata;

import java.sql.Connection;

/**
 * MySQL / MariaDB DDL executor. All generation logic lives in {@link StandardSqlDdlExecutor}; this class
 * just binds it to the {@link MariadbDialect}.
 *
 * @author tinhnv
 * @since Dec 20, 2023
 */
public class MariadbDdlExecutor extends StandardSqlDdlExecutor {

    public MariadbDdlExecutor(TableMetadata tableMetadata, Connection connection) {
        super(tableMetadata, connection, MariadbDialect.INSTANCE);
    }

}
