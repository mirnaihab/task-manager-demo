import { Router } from 'express';
import type { Pool } from 'pg';
import { logger } from '../logger';

export function createHealthRouter(pool: Pool): Router {
  const router = Router();
  // No database access. Render probes this path, so Neon can scale to zero (DES-P4-006).
  router.get('/live', (_req, res) => {
    res.json({ status: 'ok' });
  });
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
