import pg from 'pg';

const DATE_OID = 1082;

// Keep DATE columns as 'YYYY-MM-DD' strings so time zones cannot shift them.
pg.types.setTypeParser(DATE_OID, (value) => value);

export function createPool(databaseUrl: string): pg.Pool {
  return new pg.Pool({ connectionString: databaseUrl, max: 5 });
}
