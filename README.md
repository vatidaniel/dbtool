# dbtool

A small Java 11 library for **building SQL queries and executing DDL across database dialects**. It is
consumed as a dependency (e.g. by a data-governance / ETL platform) rather than run on its own.

## Features

- **Query building** (`io.github.vatisteve.dataaccess`) — fluent `BasicQuery` / `BasicCriteria` builders
  for MySQL/MariaDB-style `SELECT` statements, plus `information_schema` introspection helpers and a
  paged data-fetch contract (`ExecuteDataService`).
- **DDL execution** (`io.github.vatisteve.metadata`) — a dialect-agnostic `DdlExecutor` interface
  (create/drop/rename table, add/drop/modify columns and constraints) over a JDBC `Connection`, with
  per-dialect implementations for **MariaDB** and **ClickHouse**.
- **Typed metadata model** — `TableMetadata` / `ColumnMetadata` and a `DataType` hierarchy
  (`MariadbDataType`, `ClickHouseDataType`) rooted at the `NUMERIC / STRING / TEMPORAL / SPATIAL`
  categories.

## Requirements

- Java 11 (build with JDK 11–17; Lombok 1.18.30 predates JDK 25)
- Maven 3

## Build & test

```bash
mvn clean install   # build + install to local repo
mvn test            # run the JUnit 5 test suite
```

## Usage

Generate a `CREATE TABLE` statement from metadata:

```java
TableMetadata table = TableMetadata.builder()
    .name("person")
    .columnsMetadata(List.of(
        ColumnMetadata.builder().name("id").dataType("INT")
            .primaryKey(true).nullable(false).identity(true).build(),
        ColumnMetadata.builder().name("name").dataType("VARCHAR").dataTypeExtension("255").build()))
    .build();

try (DdlExecutor ddl = new MariadbDdlExecutor(table, connection)) {
    ddl.createTable();
}
```

Build a query:

```java
String sql = new BasicQuery()
    .select("id", "name")
    .from("`users`")
    .where(new BasicCriteria("age").equalWithNumber(30))
    .limit(10).offset(0)
    .toQueryString();
```

> **Note:** the query/DDL builders concatenate raw strings and do **not** perform parameter binding or
> identifier escaping. Only pass trusted or pre-validated input.

## Adding a dialect

Create a package under `metadata` with an executor that `extends DdlQueryConstants implements DdlExecutor`
(see `metadata.mariadb` / `metadata.clickhouse`) and a `DataType` enum for the dialect's types.
