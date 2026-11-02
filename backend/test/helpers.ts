import type { Express } from 'express';
import pg from 'pg';
import { createApp } from '../src/app';
import { createPool } from '../src/db';

export const FIXED_NOW = new Date('2026-11-02T10:00:00Z');
export const TODAY = '2026-11-02';
export const YESTERDAY = '2026-11-01';
export const TOMORROW = '2026-11-03';

export interface TestContext {
  app: Express;
  pool: pg.Pool;
}

export function createTestContext(): TestContext {
  const pool = createPool(process.env['DATABASE_URL'] ?? '');
  return { app: createApp({ pool, clock: () => FIXED_NOW }), pool };
}

export async function truncateTasks(pool: pg.Pool): Promise<void> {
  await pool.query('TRUNCATE tasks');
}

export interface SeedTask {
  title?: string;
  priority?: 'LOW' | 'MEDIUM' | 'HIGH';
  status?: 'TODO' | 'IN_PROGRESS' | 'DONE';
  dueDate?: string;
}

// Inserts directly so tests can create past-due tasks the API would refuse.
export async function seedTask(pool: pg.Pool, task: SeedTask = {}): Promise<string> {
  const { rows } = await pool.query<{ id: string }>(
    `INSERT INTO tasks (title, priority, status, due_date) VALUES ($1, $2, $3, $4) RETURNING id`,
    [task.title ?? 'Seed task', task.priority ?? 'MEDIUM', task.status ?? 'TODO', task.dueDate ?? TODAY],
  );
  return rows[0]?.id ?? '';
}
