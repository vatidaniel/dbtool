# dbtool

A small Java 11 library for **building SQL queries and executing DDL across database dialects**. It is
consumed as a dependency (e.g. by a data-governance / ETL platform) rather than run on its own.

## Features

- **Query building** (`io.github.vatisteve.dataaccess`) — fluent `BasicQuery` / `BasicCriteria` builders
  for MySQL/MariaDB-style `SELECT` statements, plus `information_schema` introspection helpers and a
  paged data-fetch contract (`ExecuteDataService`).
- **DDL execution** (`io.github.vatisteve.metadata`) — a dialect-agnostic `DdlExecutor` interface
  (create/drop/rename table, add/drop/modify columns and constraints) over a JDBC `Connection`, with
  per-dialect implementations for **MySQL/MariaDB**, **PostgreSQL**, **SQL Server**, and **ClickHouse**.
  Multiple operations can be wrapped in one transaction via `runInTransaction(...)`.
- **Typed metadata model** — `TableMetadata` / `ColumnMetadata` and a `DataType` hierarchy
  (`MariadbDataType`, `PostgresDataType`, `SqlServerDataType`, `ClickHouseDataType`) rooted at the
  `NUMERIC / STRING / TEMPORAL / SPATIAL` categories.

## Requirements

- Java 11 (builds on modern JDKs, verified on JDK 11 and 25)
- Maven 3

## Build & test

```bash
mvn clean install              # build + install to local repo
mvn test                       # run the JUnit 5 unit tests (no Docker needed)
mvn -Pintegration-tests test   # also run Testcontainers integration tests (requires Docker)
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

The executor uses the `Connection` you pass in but does **not** close it — the caller owns the
connection's lifecycle (`close()` is a no-op kept only for try-with-resources convenience).

Build a query with bound values (preferred) and run it safely:

```java
ParameterizedQuery pq = new BasicQuery()
    .select("id", "name")
    .from("`users`")
    .where(new BasicCriteria("age").equal(30).and(new BasicCriteria("city").in("NY", "LA")))
    .paginate(10, 0)
    .toPreparedQuery();              // SQL with ? placeholders + ordered values

try (PreparedStatement ps = pq.prepare(connection);
     ResultSet rs = ps.executeQuery()) {
    // ...
}
```

> **Note:** prefer the bound value methods (`equal`, `in`, ...) plus `toPreparedQuery()` so values go
> through a `PreparedStatement`. The older `*WithSingleQuote`/`*Format` methods and `toQueryString()`
> inline values into the SQL text and are deprecated. **Identifiers** (table/column names) are quoted and
> escaped per dialect but cannot be parameterized, so identifiers must still be trusted or validated; the
> DDL side likewise builds raw SQL.

## Adding a dialect

The relational DDL logic lives once in `metadata.core.StandardSqlDdlExecutor`; the parts that vary
between databases are captured by the `SqlDialect` strategy (`io.github.vatisteve.dataaccess`):
identifier quoting, the identity/auto-increment clause, the storage clause, and pagination. To add a
standard relational database:

1. Implement `SqlDialect` (see `MariadbDialect` / `PostgresDialect`).
2. Add a thin `DdlExecutor` that extends `StandardSqlDdlExecutor` and passes your dialect (see
   `metadata.mariadb.MariadbDdlExecutor` / `metadata.postgres.PostgresDdlExecutor` /
   `metadata.sqlserver.SqlServerDdlExecutor`). Override a `protected` hook such as
   `buildUpdateColumnDefinitionSql` / `buildAddConstraintSql`, or a whole operation, only where the
   statement shape genuinely differs (SQL Server, for example, overrides `addColumn`, the renames, and
   the constraint hooks).
3. Add a `DataType` enum mapping the database's types to the `NUMERIC/STRING/TEMPORAL/SPATIAL` categories
   (see `MariadbDataType` / `PostgresDataType` / `SqlServerDataType`).

`SqlQueryServiceCommon` and `BasicQuery` also take a `SqlDialect`, so read-side queries
(`information_schema` access, pagination) follow the same dialect — e.g. SQL Server's
`OFFSET ... FETCH NEXT` (use `BasicQuery.orderBy(...)`, which it requires) differs from `LIMIT/OFFSET`.
Foreign-key references in generated DDL are quoted per-dialect. Databases that diverge too far from
standard SQL can implement `DdlExecutor` directly instead of extending `StandardSqlDdlExecutor` (see
`metadata.clickhouse`).
