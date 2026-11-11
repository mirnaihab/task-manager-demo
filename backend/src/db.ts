import pg from 'pg';
import { logger } from './logger';

const DATE_OID = 1082;

// Keep DATE columns as 'YYYY-MM-DD' strings so time zones cannot shift them.
pg.types.setTypeParser(DATE_OID, (value) => value);

// A request must fail in a known time when the database does not answer (REQ-412).
const CONNECTION_TIMEOUT_MS = 10_000;

export function createPool(databaseUrl: string): pg.Pool {
  const pool = new pg.Pool({
    connectionString: databaseUrl,
    max: 5,
    connectionTimeoutMillis: CONNECTION_TIMEOUT_MS,
  });
  // Without this listener Node exits when an idle client drops.
  pool.on('error', (err) => logger.error({ err }, 'Unexpected error on idle database client'));
  return pool;
}
