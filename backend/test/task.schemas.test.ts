import { describe, expect, it } from 'vitest';
import { createTaskSchema, listQuerySchema, updateTaskSchema } from '../src/tasks/task.schemas';

const valid = { title: 'Pay rent', priority: 'HIGH', dueDate: '2026-11-02' };

describe('createTaskSchema (REQ-002)', () => {
  it('accepts a valid body', () => {
    expect(createTaskSchema.safeParse(valid).success).toBe(true);
  });
  it('EDGE-P1-001 accepts a 100 character title', () => {
    expect(createTaskSchema.safeParse({ ...valid, title: 'a'.repeat(100) }).success).toBe(true);
  });
  it('EDGE-P1-002 trims before the length check', () => {
    const result = createTaskSchema.safeParse({ ...valid, title: ` ${'a'.repeat(100)} ` });
    expect(result.success).toBe(true);
  });
  it('rejects a 101 character title', () => {
    expect(createTaskSchema.safeParse({ ...valid, title: 'a'.repeat(101) }).success).toBe(false);
  });
  it('rejects a whitespace title', () => {
    expect(createTaskSchema.safeParse({ ...valid, title: '   ' }).success).toBe(false);
  });
  it('rejects a bad priority and a missing dueDate', () => {
    expect(createTaskSchema.safeParse({ ...valid, priority: 'URGENT' }).success).toBe(false);
    expect(createTaskSchema.safeParse({ title: 'x', priority: 'LOW' }).success).toBe(false);
  });
  it('EDGE-P1-003 rejects 2026-02-30', () => {
    expect(createTaskSchema.safeParse({ ...valid, dueDate: '2026-02-30' }).success).toBe(false);
  });
  it('rejects an unknown field', () => {
    expect(createTaskSchema.safeParse({ ...valid, extra: 1 }).success).toBe(false);
  });
});

describe('updateTaskSchema (REQ-008)', () => {
  it('rejects an empty body', () => {
    expect(updateTaskSchema.safeParse({}).success).toBe(false);
  });
  it('EDGE-P1-005 accepts null description', () => {
    expect(updateTaskSchema.safeParse({ description: null }).success).toBe(true);
  });
  it('accepts a status only', () => {
    expect(updateTaskSchema.safeParse({ status: 'DONE' }).success).toBe(true);
  });
});

describe('listQuerySchema (REQ-006)', () => {
  it('accepts valid filters', () => {
    expect(listQuerySchema.safeParse({ status: 'TODO', priority: 'HIGH' }).success).toBe(true);
  });
  it('rejects an unknown value', () => {
    expect(listQuerySchema.safeParse({ status: 'BLOCKED' }).success).toBe(false);
  });
  it('EDGE-P1-011 rejects a repeated parameter', () => {
    expect(listQuerySchema.safeParse({ status: ['TODO', 'DONE'] }).success).toBe(false);
  });
});
