import { Router } from 'express';
import type { Pool } from 'pg';
import { logger } from '../logger';

export function createHealthRouter(pool: Pool): Router {
  const router = Router();
  router.get('/', async (_req, res) => {
    try {
      await pool.query('SELECT 1');
      res.json({ status: 'ok' });
    } catch (err) {
      logger.error({ err }, 'Health check failed');
      res.status(503).json({ status: 'unavailable' });
    }
  });
  return router;
}
