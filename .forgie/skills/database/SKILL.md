---
name: database
description: Comprehensive database skill for schema design, migrations, query optimization, code review, and DBA operations across SQL databases. Use when working on database design, performance, migrations or any database-related code changes.
---

# Database Specialist & Code Reviewer

You are an expert Database Administrator (DBA) and Database Reviewer. Your mission is to ensure database code follows best practices, prevents performance issues, and maintains data integrity.

## Core Responsibilities
1. **Query Performance**: Optimize queries, design proper index architectures (composite, covering, partial), and prevent table scans and N+1 patterns.
2. **Schema Design**: Design efficient schemas with proper normalization, data types, constraints (PK, FK, CHECK), and consistent naming conventions.
3. **Security**: Implement access control, SQL injection prevention, Row Level Security (RLS), and the principle of least privilege.
4. **Migrations**: Ensure zero-downtime deployments, reversible schema changes, and separate data and schema migrations.
5. **DBA Operations**: Manage connections, configure pools, perform upgrades, and utilize appropriate tooling (e.g., VS Code extensions).

## Universal Review Checklist
- **Security**: Parameterized queries, no dynamic SQL concatenations, granular permissions, protected sensitive data.
- **Performance**: WHERE/JOIN columns are indexed, exact execution plans checked (`EXPLAIN ANALYZE`), proper pagination strategies (Cursor vs OFFSET), batch operations used.
- **Maintainability**: Consistent naming conventions, optimized JOINs, subqueries converted to CTEs or Window Functions where appropriate.
- **Schema**: Tables normalized appropriately, strict data types chosen, foreign keys are always indexed.


# Database Migration Patterns & Zero-Downtime

Safe, reversible database schema changes for production systems.

## Core Principles
1. **Every change is a migration**: Never alter production databases manually.
2. **Forward-only in production**: Rollbacks must use new forward migrations.
3. **Immutability**: Never edit a migration that has run in production.
4. **Separation of Concerns**: Schema changes (DDL) and Data changes (DML) must be in separate migrations.
5. **Test to Scale**: A migration that works on 100 rows may lock on 10M rows. Test against production-sized data.

## Zero-Downtime Migration Strategy
For critical production changes, always follow the expand-contract pattern:

1. **Phase 1 (Expand)**:
   - Add new column/table (nullable or with default)
   - Deploy: app writes to BOTH old and new columns.
2. **Phase 2 (Migrate)**:
   - Run backfill migration for existing rows.
   - Deploy: app reads from NEW column, writes to BOTH.
   - Verify data consistency.
3. **Phase 3 (Contract)**:
   - Deploy: app only uses NEW column.
   - Drop old column/table in a separate migration.

## Migration Safety Checklist
Before applying any migration, verify:
- [ ] Migration has both UP and DOWN (or is explicitly marked irreversible).
- [ ] No full table locks on large tables (use concurrent operations).
- [ ] New columns have defaults or are nullable (never add `NOT NULL` without default on an existing table).
- [ ] Indexes are created concurrently (not inline with `CREATE TABLE` for existing tables).
- [ ] Data backfill is a separate migration from the schema change.
- [ ] Rollback plan documented.

## PostgreSQL Migration Patterns
- **Adding a Column Safely**: Add a nullable column, or a column with a default (Postgres 11+ makes defaults instant). `NOT NULL` without default rewrite every row blocking the table.
- **Adding an Index Without Downtime**: `CREATE INDEX CONCURRENTLY idx_users_email ON users (email);` (Note: cannot run inside a transaction block).
- **Large Data Migrations**: Use batch updates with progress using `FOR UPDATE SKIP LOCKED` inside a `DO` block to avoid locking the whole table.

```sql
-- Batch Update Example
DO $$
DECLARE
  batch_size INT := 10000;
  rows_updated INT;
BEGIN
  LOOP
    UPDATE users SET normalized_email = LOWER(email)
    WHERE id IN (
      SELECT id FROM users WHERE normalized_email IS NULL LIMIT batch_size FOR UPDATE SKIP LOCKED
    );
    GET DIAGNOSTICS rows_updated = ROW_COUNT;
    EXIT WHEN rows_updated = 0;
    COMMIT;
  END LOOP;
END $$;
```

## ORM and Tool-Specific Workflows

### Prisma (TypeScript/Node.js)
- `npx prisma migrate dev --name <name>`
- `npx prisma migrate deploy`
- `npx prisma generate`
**Custom SQL**: For operations Prisma cannot express (concurrent indexes, data backfills), use `npx prisma migrate dev --create-only --name <name>` and write the SQL manually.

### Drizzle (TypeScript/Node.js)
- `npx drizzle-kit generate`
- `npx drizzle-kit migrate`
- `npx drizzle-kit push` (development only)

### Kysely (TypeScript/Node.js)
- `kysely migrate make <name>`, `kysely migrate latest`, `kysely migrate down`.
**Rule**: Always use `Kysely<any>` in migration files, not your strongly-typed DB interface. Migrations are frozen in time and must not depend on current schema types.

### Django (Python)
- `python manage.py makemigrations`, `python manage.py migrate`.
- Use `python manage.py makemigrations --empty app_name -n description` for data migrations. Run backfills via `operations = [migrations.RunPython(backfill, reverse)]`.
- Use `SeparateDatabaseAndState` to gracefully drop fields without dropping them from the DB immediately, preventing application errors during deploys.

### golang-migrate (Go)
- `migrate create -ext sql -dir migrations -seq <name>`
- `migrate -path migrations -database "$DATABASE_URL" up`
- Clean separate `up.sql` and `down.sql` files for explicit control over indexing and schema creation.

## Common Anti-Patterns
- **Manual SQL in production**: Unrepeatable. Always use migration files.
- **Editing deployed migrations**: Causes drift between environments. Always create a new forward migration.
- **NOT NULL without default**: Locks the table. Fix: Add nullable, backfill, then add constraint.
- **Inline index on large table**: Blocks writes. Fix: Use `CONCURRENTLY`.
- **Dropping column before removing code**: Causes application errors. Fix: Remove app code first, drop column in the next deploy.


# PostgreSQL Patterns, Review & Optimization

## Diagnostic Commands
```bash
psql $DATABASE_URL
# Identify slow queries
psql -c "SELECT query, mean_exec_time, calls FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"

# Table size bloat check
psql -c "SELECT relname, pg_size_pretty(pg_total_relation_size(relid)) FROM pg_stat_user_tables ORDER BY pg_total_relation_size(relid) DESC;"

# Index usage efficiency
psql -c "SELECT indexrelname, idx_scan, idx_tup_read FROM pg_stat_user_indexes ORDER BY idx_scan DESC;"
```

## Data Types and Schema Design
- **Types**: Use `bigint` for IDs (not `int`), `text` or `citext` for strings (not `varchar(255)`), `timestamptz` for timestamps (not `timestamp`), `numeric(10,2)` for money (not `float`), `boolean` for flags. Utilize custom `DOMAIN` or `ENUM` types.
- **JSONB**: Index with GIN (`CREATE INDEX idx ON t USING gin(data)`). Use containment operators `data @> '{"status": "shipped"}'` instead of `->>` matching on strings where possible. Enforce structure via `CHECK` constraints.
- **Arrays**: Do not use `ANY()` without indexes. Use GIN indexing and `@>` operators for arrays. Bulk update rather than mutating in slow loops.
- **Constraints**: Enforce data integrity at the database level (PK, FK, `NOT NULL`, `CHECK`). Use `lowercase_snake_case` identifiers (no quoted mixed-case).
- **Always** prefer separate columns over JSON columns every time for SQL tables. If it cannot be avoided, use JSONB with GIN indexes and constraints to enforce structure.

## Advanced Features & Extensions
- **Extensions**: Check `IF NOT EXISTS` before creation (`uuid-ossp`, `pgcrypto`, `pg_trgm`). Use `crypt`, `uuid_generate_v4()`, and `word_similarity`.
- **CTE & Window Functions**: Use recursive CTEs for hierarchical data, and Window Functions for running totals/rankings (e.g., `SUM(amount) OVER (PARTITION BY ...)`).
- **Queue Processing**: Use `FOR UPDATE SKIP LOCKED` for worker queues to achieve extremely high throughput without blocking locks. (10x faster)

## Indexing Strategy Cheat Sheet
| Query Pattern | Index Type | Example |
|--------------|------------|---------|
| `WHERE col = value` | B-tree (default) | `CREATE INDEX idx ON t (col)` |
| `WHERE col > value` | B-tree | `CREATE INDEX idx ON t (col)` |
| `WHERE a = x AND b > y` | Composite | `CREATE INDEX idx ON t (a, b)` |
| `WHERE jsonb @> '{}'` | GIN | `CREATE INDEX idx ON t USING gin (col)` |
| `WHERE tsv @@ query` | GIN | `CREATE INDEX idx ON t USING gin (col)` |
| Time-series ranges | BRIN | `CREATE INDEX idx ON t USING brin (col)` |

- **Index Foreign Keys**: Always. No exceptions.
- **Composite Order**: Equality columns first, then range columns `(status, created_at)`.
- **Covering Indexes**: Use `INCLUDE (col)` to avoid main table lookups entirely.
- **Partial Indexes**: E.g., `WHERE deleted_at IS NULL` for soft deletes in active record systems.
- **Concurrent Indexed**: Create indexes with `CREATE INDEX CONCURRENTLY` on live systems avoiding locks.

## Security & Row Level Security (RLS)
- **RLS**: Implement RLS on multi-tenant tables. Wrap the check in a subquery: `USING ((SELECT auth.uid()) = user_id)`.
- **Policy Indexing**: Ensure RLS policy columns strictly have performance indexes.
- **Roles**: Revoke all on schema public from public. Grant granular permissions (`SELECT`, `INSERT`, `UPDATE`) directly to application roles, avoiding `GRANT ALL`.

## Anti-Patterns to Flag
- Unindexed foreign keys or RLS columns.
- Deep, unvalidated JSONB nesting strings instead of real types.
- Random UUIDs as primary keys without UUIDv7 sorting (slows B-tree insertion).
- Trigger functions modifying multiple rows or re-triggering unnecessarily (limit using `WHEN (OLD.* IS DISTINCT FROM NEW.*)` and `CURRENT_TIMESTAMP`).
- Holding locks during external API calls.

## Output Format for Performance Analysis
```markdown
## Query Performance Analysis

**Original Query**:
[Original SQL with performance issues]

**Issues Identified**:
- Sequential scan on large table (Cost: X)
- Missing index on frequently queried column
- Inefficient join order

**Optimized Query**:
[Improved SQL with explanations]

**Recommended Indexes**:
```sql
CREATE INDEX idx_table_column ON table(column);
```

**Performance Impact**: Expected X% improvement in execution time
```

## Configuration Template Example
```sql
ALTER SYSTEM SET max_connections = 100;
ALTER SYSTEM SET work_mem = '8MB';
ALTER SYSTEM SET idle_in_transaction_session_timeout = '30s';
ALTER SYSTEM SET statement_timeout = '30s';
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
REVOKE ALL ON SCHEMA public FROM public;
SELECT pg_reload_conf();
```



# Universal SQL Code Review & Optimization

Applies universally across MySQL, PostgreSQL, SQL Server, Oracle, and others. Focuses on performance, access controls, coding standards, subquery optimization, pagination, data types, logic structures, and secure injection handling.

## Query Performance & Structure
- **Filtering**: Filtering should happen early in `WHERE` and `JOIN` clauses. Avoid functions in `WHERE` (e.g., `UPPER(customer_email) = 'X'`) as they prevent index usage; use expressions or data types that align naturally.
- **Pagination**: Avoid `OFFSET`-based pagination on large tables. Use Cursor-based pagination (`WHERE id > $last_index` or `WHERE created_at < $cursor`).
- **Subqueries vs. CTEs / Window Functions**: Replace correlated subqueries directly with Window Functions (e.g. `AVG() OVER (PARTITION BY category)`) or efficient `JOIN` approaches to prevent severe performance penalties.
- **Aggregations**: Evaluate multiple separate aggregation queries (`COUNT WHERE...`) against conditional aggregation approaches to prevent repeated full table scans: `COUNT(CASE WHEN status = 'pending' THEN 1 END)`.
- **OR vs. UNION**: Deeply complex `OR` conditions can confuse query optimizers. Often, a `UNION ALL` provides vastly better execution plans across large disjoint criteria.
- **Distinct Misuse**: Utilizing `DISTINCT` to mask underlying Cartesian Products creates major application flaws and masks bad joins. Ensure proper `INNER JOIN`/`GROUP BY`.

## Style & Structure Best Practices
- **Aliases**: Strictly use explicit, readable column and table aliases. Avoid `SELECT *`.
- **Indentation**: Formulate code consistently—Uppercase keywords (`SELECT`, `JOIN`, `WHERE`), explicit nested line breaks for expressions.
- **Transactions**: Explicitly structure transactions (`BEGIN` to `COMMIT`), and strictly avoid long-running locks in complex procedural flows.

## Stored Procedures & Convention
- **Naming**: 
  - Standardize Stored Procedure names with `usp_` prefix and PascalCase.
  - Make names descriptive of the action (e.g. `usp_GetCustomerOrders`). Check for plurality on result sets (e.g. `Products` vs `Product`).
  - Tables and columns should strictly exist in singular form according to convention.
  - Ensure all tables encompass a primary key column named `id`, a `created_at` timestamp, and an `updated_at` tracking timestamp.
- **Parameters**:
  - Prefix query parameters explicitly (e.g., `@`). Use `camelCase`.
  - Enforce param valid/type checks initially, provide necessary defaults, and position parameters optimally (Required, Optional).
- **Architecture**:
  - Integrate a top-level header comment block containing descriptions, parameters, and output results.
  - Employ Standardized Error Codes / Messages. Use `OUTPUT` fields to transport return statuses.
  - For massive transformations, prefix temp tables with `tmp_`.
  - Include `SET NOCOUNT ON` to optimize network overhead for DML statements.

## Performance Analysis & Tooling Strategies
**Identify Sluggish Code:** Gather insights universally per environment:
- **MySQL**: Read `mysql.slow_log` using `query_time` and `rows_sent`.
- **PostgreSQL**: Interrogate `pg_stat_statements` referencing execution counts and total time.
- **SQL Server**: Leverage Dynamic Management Views (DMVs) (`sys.dm_exec_query_stats` + `sys.dm_exec_sql_text`).

## Anti-Pattern Detection Strategies
- **Batch Processing Failure**: Instead of loops processing single inputs per database query (the N+1 anti-pattern), employ `INSERT` batches (`VALUES (x),(y)` combinations).
- **Index Fragments**: Do not over-index. It rapidly impacts `INSERT` and `UPDATE` latencies. Focus entirely on `Composite` indexes sorted optimally for data conditions, or `Covering` indices leveraging SQL Server's `INCLUDE`.
- **Partial Indices**: Build targeted smaller indices for specific conditional statuses (`WHERE status IN ('pending')`).
- **Dynamic Risk**: Absolutely no un-parameterized or string-concatenation dynamically generated SQL (`query = f"DELETE FROM table WHERE id = {var}"`). Validate security consistently.

## Code Review Structure & Output Output Format

### Output Template: Quality Review
```markdown
## [PRIORITY] [CATEGORY]: [Brief Description]

**Location**: [Table/View/Procedure name and line number if applicable]
**Issue**: [Detailed explanation of the problem]
**Security Risk**: [If applicable - injection risk, data exposure, etc.]
**Performance Impact**: [Query cost, execution time impact]
**Recommendation**: [Specific fix with code example]

**Before**:
```sql
-- Problematic SQL
```

**After**:
```sql
-- Improved SQL
```

**Expected Improvement**: [Performance gain, security benefit]

### Quality Assessment Checklists to Run
- **Security Score**: [1-10] (SQL injection protection, Role-base access control, Principle of least privilege)
- **Performance Score**: [1-10] (Eliminated full scans, Cartesian products, inefficient indices, appropriate JOIN usages)
- **Maintainability Score**: [1-10] (Names, Comments, Conventions, Format styles)
- **Schema Quality Score**: [1-10] (Normalization level, Constraint presence, Default usage)
```