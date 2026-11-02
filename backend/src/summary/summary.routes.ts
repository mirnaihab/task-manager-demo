import { Router } from 'express';
import type { TaskService } from '../tasks/task.service';

export function createSummaryRouter(service: TaskService): Router {
  const router = Router();
  router.get('/', async (_req, res) => {
    res.json(await service.summary());
  });
  return router;
}
