import request from 'supertest';
import { afterAll, beforeEach, describe, expect, it } from 'vitest';
import { createTestContext, seedTask, truncateTasks, TODAY, TOMORROW, YESTERDAY } from './helpers';

const { app, pool } = createTestContext();
const UUID = '22222222-2222-4222-8222-222222222222';

beforeEach(() => truncateTasks(pool));
afterAll(() => pool.end());

const validBody = { title: 'Pay rent', priority: 'HIGH', dueDate: TODAY };
const titles = (body: { title: string }[]): string[] => body.map((t) => t.title);

describe('POST /api/tasks', () => {
  it('REQ-001 creates a task', async () => {
    const res = await request(app).post('/api/tasks').send(validBody);
    expect(res.status).toBe(201);
    expect(res.body).toMatchObject({ status: 'TODO', overdue: false, dueDate: TODAY, description: null });
    expect(res.body.id).toMatch(/^[0-9a-f-]{36}$/);
  });
  it('REQ-002 rejects a 101 character title', async () => {
    const res = await request(app).post('/api/tasks').send({ ...validBody, title: 'a'.repeat(101) });
    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe('VALIDATION_ERROR');
    expect(res.body.error.fields.title).toBeDefined();
  });
  it('REQ-002 rejects a whitespace title', async () => {
    const res = await request(app).post('/api/tasks').send({ ...validBody, title: '   ' });
    expect(res.status).toBe(400);
    expect(res.body.error.fields.title).toBeDefined();
  });
  it('REQ-003 rejects a past due date', async () => {
    const res = await request(app).post('/api/tasks').send({ ...validBody, dueDate: YESTERDAY });
    expect(res.status).toBe(400);
    expect(res.body.error.fields.dueDate).toBeDefined();
  });
  it('REQ-016 stores a blank description as null', async () => {
    const res = await request(app).post('/api/tasks').send({ ...validBody, description: '  ' });
    expect(res.body.description).toBeNull();
  });
  it('EDGE-P1-008 maps malformed JSON to 400', async () => {
    const res = await request(app).post('/api/tasks').set('Content-Type', 'application/json').send('{bad');
    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe('VALIDATION_ERROR');
  });
  it('EDGE-P1-009 maps a large body to 400', async () => {
    const res = await request(app).post('/api/tasks').send({ ...validBody, description: 'a'.repeat(20000) });
    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe('VALIDATION_ERROR');
  });
});

describe('GET /api/tasks', () => {
  it('REQ-005 combines filters with AND', async () => {
    await seedTask(pool, { title: 'a', status: 'TODO', priority: 'HIGH' });
    await seedTask(pool, { title: 'b', status: 'TODO', priority: 'LOW' });
    await seedTask(pool, { title: 'c', status: 'DONE', priority: 'HIGH' });
    const res = await request(app).get('/api/tasks?status=TODO&priority=HIGH');
    expect(res.status).toBe(200);
    expect(titles(res.body)).toEqual(['a']);
  });
  it('REQ-006 rejects a bad filter', async () => {
    const res = await request(app).get('/api/tasks?status=BLOCKED');
    expect(res.status).toBe(400);
    expect(res.body.error.fields.status).toBeDefined();
  });
  it('EDGE-P1-011 rejects a repeated filter', async () => {
    const res = await request(app).get('/api/tasks?status=TODO&status=DONE');
    expect(res.status).toBe(400);
  });
  it('returns an empty array when nothing matches', async () => {
    const res = await request(app).get('/api/tasks?status=DONE');
    expect(res.status).toBe(200);
    expect(res.body).toEqual([]);
  });
  it('REQ-004 sorts by due date then creation', async () => {
    await seedTask(pool, { title: 'late', dueDate: TOMORROW });
    await seedTask(pool, { title: 'early', dueDate: YESTERDAY });
    const res = await request(app).get('/api/tasks');
    expect(titles(res.body)).toEqual(['early', 'late']);
    expect(res.body[0].overdue).toBe(true);
  });
  it('CON-002 returns dueDate as a string', async () => {
    await seedTask(pool, { dueDate: '2026-12-31' });
    const res = await request(app).get('/api/tasks');
    expect(res.body[0].dueDate).toBe('2026-12-31');
  });
});

describe('PATCH /api/tasks/:id', () => {
  it('REQ-007 status change persists', async () => {
    const id = await seedTask(pool);
    const res = await request(app).patch(`/api/tasks/${id}`).send({ status: 'DONE' });
    expect(res.status).toBe(200);
    expect((await request(app).get('/api/tasks')).body[0].status).toBe('DONE');
  });
  it('REQ-009 renames an overdue task', async () => {
    const id = await seedTask(pool, { dueDate: YESTERDAY });
    const res = await request(app).patch(`/api/tasks/${id}`).send({ title: 'New' });
    expect(res.status).toBe(200);
    expect(res.body.title).toBe('New');
  });
  it('REQ-009 rejects moving the due date to the past', async () => {
    const id = await seedTask(pool, { dueDate: TODAY });
    const res = await request(app).patch(`/api/tasks/${id}`).send({ dueDate: YESTERDAY });
    expect(res.status).toBe(400);
  });
  it('EDGE-P1-005 clears the description with null', async () => {
    const id = await seedTask(pool);
    await request(app).patch(`/api/tasks/${id}`).send({ description: 'x' });
    const res = await request(app).patch(`/api/tasks/${id}`).send({ description: null });
    expect(res.body.description).toBeNull();
  });
  it('REQ-008 rejects an empty body', async () => {
    const id = await seedTask(pool);
    const res = await request(app).patch(`/api/tasks/${id}`).send({});
    expect(res.status).toBe(400);
  });
  it('REQ-011 returns 404 for an unknown or malformed id', async () => {
    const unknown = await request(app).patch(`/api/tasks/${UUID}`).send({ title: 'x' });
    expect(unknown.status).toBe(404);
    expect(unknown.body.error.code).toBe('NOT_FOUND');
    const malformed = await request(app).patch('/api/tasks/not-a-uuid').send({ title: 'x' });
    expect(malformed.status).toBe(404);
  });
});

describe('DELETE /api/tasks/:id', () => {
  it('REQ-010 deletes a task', async () => {
    const id = await seedTask(pool);
    expect((await request(app).delete(`/api/tasks/${id}`)).status).toBe(204);
    expect((await request(app).get('/api/tasks')).body).toEqual([]);
  });
  it('REQ-011 returns 404 for an unknown id', async () => {
    const res = await request(app).delete(`/api/tasks/${UUID}`);
    expect(res.status).toBe(404);
    expect(res.body.error.code).toBe('NOT_FOUND');
  });
});

describe('unknown route', () => {
  it('EDGE-P1-012 returns 404 NOT_FOUND', async () => {
    const res = await request(app).get('/nope');
    expect(res.status).toBe(404);
    expect(res.body.error.code).toBe('NOT_FOUND');
  });
});
