import { AppError } from '../errors';
import type { TaskRepository } from './task.repository';
import type { CreateTask, Summary, Task, TaskFilters, TaskRow, UpdateTask } from './task.schemas';

export interface TaskService {
  create(input: CreateTask): Promise<Task>;
  list(filters: TaskFilters): Promise<Task[]>;
  update(id: string, patch: UpdateTask): Promise<Task>;
  remove(id: string): Promise<void>;
  summary(): Promise<Summary>;
}

const PAST_DUE_DATE = 'Due date cannot be in the past';

// A blank description is stored as null (REQ-016).
function normalizeDescription(value: string | null | undefined): string | null {
  if (value === undefined || value === null) return null;
  return value.trim() === '' ? null : value;
}

export function createTaskService(repo: TaskRepository, clock: () => Date): TaskService {
  // One place defines "today": the UTC date (A-2).
  const today = (): string => clock().toISOString().slice(0, 10);

  const withOverdue = (row: TaskRow): Task => ({
    ...row,
    overdue: row.dueDate < today() && row.status !== 'DONE',
  });

  return {
    async create(input) {
      if (input.dueDate < today()) throw AppError.validation({ dueDate: PAST_DUE_DATE });
      const row = await repo.create({ ...input, description: normalizeDescription(input.description) });
      return withOverdue(row);
    },

    async list(filters) {
      const rows = await repo.list(filters);
      return rows.map(withOverdue);
    },

    async update(id, patch) {
      const current = await repo.findById(id);
      if (!current) throw AppError.notFound();
      // The past-date rule applies only when the due date changes (A-3).
      if (patch.dueDate !== undefined && patch.dueDate !== current.dueDate && patch.dueDate < today()) {
        throw AppError.validation({ dueDate: PAST_DUE_DATE });
      }
      const normalized: UpdateTask =
        patch.description === undefined ? patch : { ...patch, description: normalizeDescription(patch.description) };
      const row = await repo.update(id, normalized);
      if (!row) throw AppError.notFound();
      return withOverdue(row);
    },

    async remove(id) {
      const removed = await repo.remove(id);
      if (!removed) throw AppError.notFound();
    },

    summary() {
      return repo.summary(today());
    },
  };
}
