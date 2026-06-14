package io.github.vatisteve.metadata.mariadb;

import io.github.vatisteve.dataaccess.MariadbDialect;
import io.github.vatisteve.metadata.core.StandardSqlDdlExecutor;
import io.github.vatisteve.metadata.core.TableMetadata;

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
