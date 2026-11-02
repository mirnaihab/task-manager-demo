import type pg from 'pg';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createPool } from '../src/db';
import { logger } from '../src/logger';

const FAKE_URL = 'postgresql://user:secret@localhost:1/none';

describe('createPool error handling (TD-001)', () => {
  let pool: pg.Pool;

  beforeEach(() => {
    pool = createPool(FAKE_URL);
  });

  afterEach(async () => {
    await pool.end();
    vi.restoreAllMocks();
  });

  it('attaches one error listener to the pool', () => {
    expect(pool.listenerCount('error')).toBe(1);
  });

  it('logs a pool error and does not throw', () => {
    const logError = vi.spyOn(logger, 'error').mockImplementation(() => undefined);
    const error = new Error('connection terminated');

    expect(() => pool.emit('error', error)).not.toThrow();

    expect(logError).toHaveBeenCalledTimes(1);
    expect(logError.mock.calls[0]?.[0]).toEqual({ err: error });
  });

  it('does not write the database URL or password to the log', () => {
    const logError = vi.spyOn(logger, 'error').mockImplementation(() => undefined);

    pool.emit('error', new Error('connection terminated'));

    const logged = JSON.stringify(logError.mock.calls);
    expect(logged).not.toContain('secret');
    expect(logged).not.toContain(FAKE_URL);
  });
});
