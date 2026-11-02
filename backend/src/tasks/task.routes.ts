import { Router } from 'express';
import { z } from 'zod';
import { AppError } from '../errors';
import { createTaskSchema, listQuerySchema, updateTaskSchema } from './task.schemas';
import type { TaskService } from './task.service';

// A malformed ID is reported as not found, so a client cannot tell the two apart (DES-P1-006).
function parseId(raw: unknown): string {
  const result = z.uuid().safeParse(raw);
  if (!result.success) throw AppError.notFound();
  return result.data;
}

export function createTaskRouter(service: TaskService): Router {
  const router = Router();

  router.post('/', async (req, res) => {
    const input = createTaskSchema.parse(req.body);
    res.status(201).json(await service.create(input));
  });

  router.get('/', async (req, res) => {
    const filters = listQuerySchema.parse(req.query);
    res.json(await service.list(filters));
  });

  router.patch('/:id', async (req, res) => {
    const id = parseId(req.params['id']);
    const patch = updateTaskSchema.parse(req.body);
    res.json(await service.update(id, patch));
  });

  router.delete('/:id', async (req, res) => {
    await service.remove(parseId(req.params['id']));
    res.status(204).end();
  });

  return router;
}
