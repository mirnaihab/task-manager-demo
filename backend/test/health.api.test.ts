import request from 'supertest';
import { afterAll, describe, expect, it, vi } from 'vitest';
import { createApp } from '../src/app';
import { createPool } from '../src/db';
import { logger } from '../src/logger';
import { createTestContext, FIXED_NOW } from './helpers';

const { app, pool } = createTestContext();

afterAll(() => pool.end());

describe('GET /health', () => {
  it('REQ-014 returns ok when the database answers', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ status: 'ok' });
  });
  it('REQ-014 returns 503 when the database is down', async () => {
    const deadPool = createPool('postgresql://localhost:1/none');
    const res = await request(createApp({ pool: deadPool, clock: () => FIXED_NOW })).get('/health');
    await deadPool.end();
    expect(res.status).toBe(503);
    expect(res.body).toEqual({ status: 'unavailable' });
  });
  it('DES-P4-006 /health/live is 200 and does not touch the database', async () => {
    const deadPool = createPool('postgresql://localhost:1/none');
    const query = vi.spyOn(deadPool, 'query');
    const deadApp = createApp({ pool: deadPool, clock: () => FIXED_NOW });
    const live = await request(deadApp).get('/health/live');
    expect(query).not.toHaveBeenCalled();
    const full = await request(deadApp).get('/health');
    await deadPool.end();
    expect(live.status).toBe(200);
    expect(live.body).toEqual({ status: 'ok' });
    expect(full.status).toBe(503);
  });
  it('DES-P4-006 does not log health requests', async () => {
    const info = vi.spyOn(logger, 'info').mockImplementation(() => undefined);
    await request(app).get('/health');
    await request(app).get('/health/live');
    expect(info).not.toHaveBeenCalled();
    info.mockRestore();
  });
  it('EDGE-P1-010 hides internals on 500 and has no stack', async () => {
    const deadPool = createPool('postgresql://localhost:1/none');
    const res = await request(createApp({ pool: deadPool, clock: () => FIXED_NOW })).get('/api/tasks');
    await deadPool.end();
    expect(res.status).toBe(500);
    expect(res.body).toEqual({ error: { code: 'INTERNAL', message: 'Something went wrong' } });
  });
});
