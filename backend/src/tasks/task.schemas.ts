import { z } from 'zod';

export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'] as const;
export const STATUSES = ['TODO', 'IN_PROGRESS', 'DONE'] as const;

const prioritySchema = z.enum(PRIORITIES, { error: 'Priority must be LOW, MEDIUM or HIGH' });
const statusSchema = z.enum(STATUSES, { error: 'Status must be TODO, IN_PROGRESS or DONE' });

// Trim first so 100 characters plus spaces is accepted (EDGE-P1-002).
const titleSchema = z
  .string({ error: 'Title is required' })
  .trim()
  .min(1, 'Title is required')
  .max(100, 'Title must be 100 characters or fewer');

const dueDateSchema = z.iso.date({ error: 'Due date must be a real date in YYYY-MM-DD format' });

const descriptionSchema = z.string({ error: 'Description must be text' }).nullable();

export const createTaskSchema = z
  .object({
    title: titleSchema,
    description: descriptionSchema.optional(),
    priority: prioritySchema,
    dueDate: dueDateSchema,
  })
  .strict();

export const updateTaskSchema = z
  .object({
    title: titleSchema.optional(),
    description: descriptionSchema.optional(),
    priority: prioritySchema.optional(),
    status: statusSchema.optional(),
    dueDate: dueDateSchema.optional(),
  })
  .strict()
  .refine((patch) => Object.values(patch).some((value) => value !== undefined), {
    error: 'At least one field is required',
  });

export const listQuerySchema = z
  .object({
    status: statusSchema.optional(),
    priority: prioritySchema.optional(),
  })
  .strict();

export type Priority = (typeof PRIORITIES)[number];
export type Status = (typeof STATUSES)[number];
export type CreateTask = z.infer<typeof createTaskSchema>;
export type UpdateTask = z.infer<typeof updateTaskSchema>;
export type TaskFilters = z.infer<typeof listQuerySchema>;

export interface TaskRow {
  id: string;
  title: string;
  description: string | null;
  priority: Priority;
  status: Status;
  dueDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface Task extends TaskRow {
  overdue: boolean;
}

export interface Summary {
  total: number;
  todo: number;
  inProgress: number;
  done: number;
  overdue: number;
}
