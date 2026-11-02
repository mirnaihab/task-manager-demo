import request from 'supertest';
import { afterAll, beforeEach, describe, expect, it } from 'vitest';
import { createTestContext, seedTask, truncateTasks, TODAY, YESTERDAY } from './helpers';

const { app, pool } = createTestContext();

beforeEach(() => truncateTasks(pool));
afterAll(() => pool.end());

describe('GET /api/summary', () => {
  it('REQ-012 returns zeros when empty', async () => {
    const res = await request(app).get('/api/summary');
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ total: 0, todo: 0, inProgress: 0, done: 0, overdue: 0 });
  });
  it('REQ-012 counts each status', async () => {
    await seedTask(pool, { status: 'TODO', dueDate: TODAY });
    await seedTask(pool, { status: 'IN_PROGRESS', dueDate: TODAY });
    await seedTask(pool, { status: 'DONE', dueDate: TODAY });
    const res = await request(app).get('/api/summary');
    expect(res.body).toEqual({ total: 3, todo: 1, inProgress: 1, done: 1, overdue: 0 });
  });
  it('REQ-013 overdue excludes DONE', async () => {
    await seedTask(pool, { status: 'TODO', dueDate: YESTERDAY });
    await seedTask(pool, { status: 'DONE', dueDate: YESTERDAY });
    const res = await request(app).get('/api/summary');
    expect(res.body.overdue).toBe(1);
  });
});
