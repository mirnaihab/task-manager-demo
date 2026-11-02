import type { Pool } from 'pg';
import type { CreateTask, Summary, TaskFilters, TaskRow, UpdateTask } from './task.schemas';

export interface TaskRepository {
  create(input: CreateTask & { description: string | null }): Promise<TaskRow>;
  list(filters: TaskFilters): Promise<TaskRow[]>;
  findById(id: string): Promise<TaskRow | null>;
  update(id: string, patch: UpdateTask): Promise<TaskRow | null>;
  remove(id: string): Promise<boolean>;
  summary(today: string): Promise<Summary>;
}

const COLUMNS = `id, title, description, priority, status,
  due_date AS "dueDate",
  created_at AS "createdAt",
  updated_at AS "updatedAt"`;

// Fixed allow-list: API field to column name. User input never becomes SQL text (SEC-001).
const UPDATE_COLUMNS = {
  title: 'title',
  description: 'description',
  priority: 'priority',
  status: 'status',
  dueDate: 'due_date',
} as const satisfies Record<keyof UpdateTask, string>;

interface DbTaskRow extends Omit<TaskRow, 'createdAt' | 'updatedAt'> {
  createdAt: Date;
  updatedAt: Date;
}

function toTaskRow(row: DbTaskRow): TaskRow {
  return { ...row, createdAt: row.createdAt.toISOString(), updatedAt: row.updatedAt.toISOString() };
}

export class PgTaskRepository implements TaskRepository {
  constructor(private readonly pool: Pool) {}

  async create(input: CreateTask & { description: string | null }): Promise<TaskRow> {
    const { rows } = await this.pool.query<DbTaskRow>(
      `INSERT INTO tasks (title, description, priority, due_date)
       VALUES ($1, $2, $3, $4)
       RETURNING ${COLUMNS}`,
      [input.title, input.description, input.priority, input.dueDate],
    );
    return toTaskRow(requireRow(rows[0]));
  }

  async list(filters: TaskFilters): Promise<TaskRow[]> {
    const conditions: string[] = [];
    const values: string[] = [];
    if (filters.status) {
      values.push(filters.status);
      conditions.push(`status = $${values.length}`);
    }
    if (filters.priority) {
      values.push(filters.priority);
      conditions.push(`priority = $${values.length}`);
    }
    const where = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';
    const { rows } = await this.pool.query<DbTaskRow>(
      `SELECT ${COLUMNS} FROM tasks ${where} ORDER BY due_date, created_at, id`,
      values,
    );
    return rows.map(toTaskRow);
  }

  async findById(id: string): Promise<TaskRow | null> {
    const { rows } = await this.pool.query<DbTaskRow>(`SELECT ${COLUMNS} FROM tasks WHERE id = $1`, [id]);
    return rows[0] ? toTaskRow(rows[0]) : null;
  }

  async update(id: string, patch: UpdateTask): Promise<TaskRow | null> {
    const assignments: string[] = [];
    const values: unknown[] = [];
    for (const key of Object.keys(UPDATE_COLUMNS) as (keyof UpdateTask)[]) {
      const value = patch[key];
      if (value === undefined) continue;
      values.push(value);
      assignments.push(`${UPDATE_COLUMNS[key]} = $${values.length}`);
    }
    values.push(id);
    const { rows } = await this.pool.query<DbTaskRow>(
      `UPDATE tasks SET ${assignments.join(', ')}, updated_at = now()
       WHERE id = $${values.length}
       RETURNING ${COLUMNS}`,
      values,
    );
    return rows[0] ? toTaskRow(rows[0]) : null;
  }

  async remove(id: string): Promise<boolean> {
    const { rowCount } = await this.pool.query('DELETE FROM tasks WHERE id = $1', [id]);
    return (rowCount ?? 0) > 0;
  }

  async summary(today: string): Promise<Summary> {
    const { rows } = await this.pool.query<Summary>(
      `SELECT
         COUNT(*)::int AS total,
         (COUNT(*) FILTER (WHERE status = 'TODO'))::int AS todo,
         (COUNT(*) FILTER (WHERE status = 'IN_PROGRESS'))::int AS "inProgress",
         (COUNT(*) FILTER (WHERE status = 'DONE'))::int AS done,
         (COUNT(*) FILTER (WHERE due_date < $1::date AND status <> 'DONE'))::int AS overdue
       FROM tasks`,
      [today],
    );
    return requireRow(rows[0]);
  }
}

function requireRow<T>(row: T | undefined): T {
  if (row === undefined) throw new Error('Query returned no row');
  return row;
}
