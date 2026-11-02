import express, { type Express, type RequestHandler } from 'express';
import helmet from 'helmet';
import type { Pool } from 'pg';
import { errorHandler, notFoundHandler } from './errorHandler';
import { createHealthRouter } from './health/health.routes';
import { logger } from './logger';
import { createSummaryRouter } from './summary/summary.routes';
import { createTaskRouter } from './tasks/task.routes';
import { PgTaskRepository } from './tasks/task.repository';
import { createTaskService } from './tasks/task.service';

export interface AppDeps {
  pool: Pool;
  clock: () => Date;
}

// Logs method, path, status and duration only. Never the body (SEC-004).
const requestLog: RequestHandler = (req, res, next) => {
  if (req.path === '/health') {
    next();
    return;
  }
  const start = process.hrtime.bigint();
  res.on('finish', () => {
    const durationMs = Number(process.hrtime.bigint() - start) / 1e6;
    logger.info(
      { method: req.method, path: req.path, status: res.statusCode, duration_ms: Math.round(durationMs) },
      'Request completed',
    );
  });
  next();
};

export function createApp({ pool, clock }: AppDeps): Express {
  const service = createTaskService(new PgTaskRepository(pool), clock);
  const app = express();

  app.disable('x-powered-by');
  app.set('trust proxy', 1);
  app.use(helmet());
  app.use(express.json({ limit: '16kb' }));
  app.use(requestLog);

  app.use('/health', createHealthRouter(pool));
  app.use('/api/tasks', createTaskRouter(service));
  app.use('/api/summary', createSummaryRouter(service));

  app.use(notFoundHandler);
  app.use(errorHandler);
  return app;
}
