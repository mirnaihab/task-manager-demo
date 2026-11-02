import { beforeEach, describe, expect, it } from 'vitest';
import { AppError } from '../src/errors';
import type { TaskRepository } from '../src/tasks/task.repository';
import type { Summary, TaskRow } from '../src/tasks/task.schemas';
import { createTaskService, type TaskService } from '../src/tasks/task.service';

const NOW = new Date('2026-11-02T10:00:00Z');
const ID = '11111111-1111-4111-8111-111111111111';

function makeRow(overrides: Partial<TaskRow> = {}): TaskRow {
  return {
    id: ID,
    title: 'T',
    description: null,
    priority: 'MEDIUM',
    status: 'TODO',
    dueDate: '2026-11-02',
    createdAt: NOW.toISOString(),
    updatedAt: NOW.toISOString(),
    ...overrides,
  };
}

class FakeRepo implements TaskRepository {
  rows = new Map<string, TaskRow>();
  async create(input: Parameters<TaskRepository['create']>[0]) {
    const row = makeRow({ ...input });
    this.rows.set(row.id, row);
    return row;
  }
  async list() {
    return [...this.rows.values()];
  }
  async findById(id: string) {
    return this.rows.get(id) ?? null;
  }
  async update(id: string, patch: Parameters<TaskRepository['update']>[1]) {
    const current = this.rows.get(id);
    if (!current) return null;
    const next = { ...current, ...patch } as TaskRow;
    this.rows.set(id, next);
    return next;
  }
  async remove(id: string) {
    return this.rows.delete(id);
  }
  async summary(): Promise<Summary> {
    return { total: 0, todo: 0, inProgress: 0, done: 0, overdue: 0 };
  }
}

describe('task service', () => {
  let repo: FakeRepo;
  let service: TaskService;

  beforeEach(() => {
    repo = new FakeRepo();
    service = createTaskService(repo, () => NOW);
  });

  it('REQ-003 rejects a past due date on create', async () => {
    await expect(
      service.create({ title: 'x', priority: 'LOW', dueDate: '2026-11-01' }),
    ).rejects.toMatchObject({ code: 'VALIDATION_ERROR', fields: { dueDate: expect.any(String) } });
  });

  it('EDGE-P1-004 accepts today and is not overdue', async () => {
    const task = await service.create({ title: 'x', priority: 'LOW', dueDate: '2026-11-02' });
    expect(task.overdue).toBe(false);
  });

  it('REQ-016 stores a blank description as null', async () => {
    const task = await service.create({ title: 'x', priority: 'LOW', dueDate: '2026-11-02', description: '   ' });
    expect(task.description).toBeNull();
  });

  it('REQ-013 flags overdue only when not DONE', async () => {
    repo.rows.set('a', makeRow({ id: 'a', dueDate: '2026-11-01' }));
    repo.rows.set('b', makeRow({ id: 'b', dueDate: '2026-11-01', status: 'DONE' }));
    const tasks = await service.list({});
    expect(tasks.find((t) => t.id === 'a')?.overdue).toBe(true);
    expect(tasks.find((t) => t.id === 'b')?.overdue).toBe(false);
  });

  it('REQ-009 allows a rename on an overdue task', async () => {
    repo.rows.set(ID, makeRow({ dueDate: '2026-10-01' }));
    const task = await service.update(ID, { title: 'New' });
    expect(task.title).toBe('New');
  });

  it('REQ-009 allows PATCH with an unchanged past dueDate', async () => {
    repo.rows.set(ID, makeRow({ dueDate: '2026-10-01' }));
    await expect(service.update(ID, { dueDate: '2026-10-01' })).resolves.toBeDefined();
  });

  it('REQ-009 rejects moving the due date to the past', async () => {
    repo.rows.set(ID, makeRow());
    await expect(service.update(ID, { dueDate: '2026-11-01' })).rejects.toMatchObject({ status: 400 });
  });

  it('EDGE-P1-006 normalizes a blank description on update', async () => {
    repo.rows.set(ID, makeRow({ description: 'old' }));
    const task = await service.update(ID, { description: '  ' });
    expect(task.description).toBeNull();
  });

  it('REQ-011 throws NOT_FOUND for unknown ids', async () => {
    await expect(service.update(ID, { title: 'x' })).rejects.toBeInstanceOf(AppError);
    await expect(service.remove(ID)).rejects.toMatchObject({ code: 'NOT_FOUND', status: 404 });
  });
});
