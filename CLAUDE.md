# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

`io.github.vatisteve:dbtool` is a Java 11 Maven **library** (packaged as a jar, no `main`). It provides
database-dialect-aware building blocks for two concerns: read-side SQL query construction / data fetching
(`dataaccess`) and write-side DDL execution against a live JDBC `Connection` (`metadata`). It is consumed
as a dependency by a larger data-governance / ETL platform (see SLF4J log topics like
`DATAGOV.DB_ENGINE.METADATA...`); this repo is just the toolkit, not the application.

## Build & Test

```bash
mvn clean install      # compile + install to local repo
mvn compile            # compile only
mvn package            # build the jar
```

There is currently **no test source tree** (`src/test`) and no tests to run. Lombok is a `provided`
annotation processor — if compilation fails with "cannot find symbol" on getters/builders, the Lombok
processor isn't running in your editor/build.

## Architecture

Two largely independent subsystems under `io.github.vatisteve`:

### `dataaccess` — query building & data fetching (read side)
- `SqlQueryConstants` is the shared base of constants and static SQL-formatting helpers (`backtickWrap`,
  `singleQuoteWrap`, `roundBracketWrap`, etc.). **Most other query/DDL classes extend it** to inherit
  these — this is the project's convention for string-assembly helpers rather than a utility class.
- `BasicQuery` + `BasicCriteria` are fluent, mutable `StringBuilder`-backed SQL builders (MariaDB/MySQL
  syntax). They concatenate strings directly — there is **no parameter binding / prepared-statement
  escaping**, so inputs must be trusted or pre-validated.
- `ExecuteDataService` is the fetch-side interface (columns, paged values, counts, `DataStructDto`).
  Domain identifiers (`dsTableId`, `versionId`) refer to the consuming platform's data-standardization
  tables, not to anything in this repo.
- `SqlQueryServiceCommon` builds `information_schema`-based introspection queries.

### `metadata` — DDL execution (write side)
- `metadata.core` holds the dialect-agnostic contract and model:
  - `DdlExecutor` (interface, `AutoCloseable`): create/drop/rename table, add/drop/rename/modify column,
    add/drop constraint. It owns a JDBC `Connection` and a `TableMetadata`, and provides a default
    `executeSql(String)` that logs then runs a `Statement`.
  - Model classes: `TableMetadata`, `ColumnMetadata` (+ nested `DefaultColumnValue`),
    `ReferenceMetadata`, `ConstraintType`, `ReferenceActionType`, `MetadataState`. These are Lombok
    `@Data`/`@Builder` POJOs and define **copy constructors** for deep cloning (`new TableMetadata(other)`).
  - `DdlQueryConstants extends SqlQueryConstants` adds DDL keyword constants; dialect executors extend it.
- **Standard relational dialects share one executor.** `metadata.core.StandardSqlDdlExecutor` holds the
  SQL-standard generation logic and delegates the bits that vary to a `SqlDialect` strategy
  (`io.github.vatisteve.dataaccess`: `quoteIdentifier`, `autoIncrementClause`, `tablespaceClause`,
  `paginate`). `MariadbDialect` / `PostgresDialect` are the impls. The executors
  (`metadata.mariadb.MariadbDdlExecutor`, `metadata.postgres.PostgresDdlExecutor`) are thin subclasses
  that just bind a dialect, overriding a `protected` hook (e.g. `buildUpdateColumnDefinitionSql`) only
  where the statement shape diverges (Postgres `ALTER COLUMN ... TYPE` vs MySQL `MODIFY`). **To add a
  standard relational DB: implement `SqlDialect`, subclass `StandardSqlDdlExecutor`, add a `DataType`
  enum.**
- **`SqlDialect` lives in `dataaccess`** (the lowest-level package) so both the DDL executors and the
  read side use it — `SqlQueryServiceCommon` takes a `SqlDialect` (defaulting to `MariadbDialect`) for
  `information_schema` quoting and pagination.
- **Genuinely divergent databases implement `DdlExecutor` directly** instead of extending the standard
  base: `metadata.clickhouse.ClickHouseDdlExecutor` (no foreign keys/identity/unique, `Nullable(...)`
  wrapping, `ENGINE`/`ORDER BY`) still `extends DdlQueryConstants implements DdlExecutor`. Several
  constraint operations across dialects throw `UnsupportedOperationException` where not yet implemented.

### `DataType` hierarchy (shared by metadata)
- `metadata.core.DataType` is an interface extending `common.EnumResponse`. Each dialect provides an
  **enum** implementing it (`ClickHouseDataType`, `MariadbDataType`) where every constant carries a
  `keyWord`, a `parent` `DataType`, and a `description`.
- `DataType.BasicDataType` (`NUMERIC, STRING, TEMPORAL, SPATIAL`) is the root category. Dialect enums set
  their `parent` to one of these so executors can branch on category (e.g. quote STRING/SPATIAL defaults,
  leave NUMERIC/TEMPORAL bare) via `getParent() instanceof DataType.BasicDataType`.
- `common.EnumResponse` (`@JsonFormat(shape = OBJECT)`) is how enums serialize to JSON as
  `{label, value}` objects for the consuming API.

## Conventions

- Every source file carries an `@author tinhnv` / `@since <date>` Javadoc header; keep this style on new files.
- String-building helpers come from `SqlQueryConstants`/`DdlQueryConstants` — reuse `backtickWrap` etc.
  instead of hand-writing quoting.
- Logging is SLF4J; DDL executors log generated SQL at `trace` via `logSqlQuery`.
