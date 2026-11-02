import { createApp } from './app';
import { loadConfig } from './config';
import { createPool } from './db';
import { logger } from './logger';

function main(): void {
  const config = loadConfig(process.env);
  const pool = createPool(config.databaseUrl);
  const app = createApp({ pool, clock: () => new Date() });

  const server = app.listen(config.port, () => {
    logger.info({ port: config.port, nodeEnv: config.nodeEnv }, 'Server started');
  });

  const shutdown = (signal: string): void => {
    logger.info({ signal }, 'Shutdown started');
    server.close(() => {
      pool
        .end()
        .then(() => process.exit(0))
        .catch((err: unknown) => {
          logger.error({ err }, 'Pool close failed');
          process.exit(1);
        });
    });
  };
  process.on('SIGTERM', () => shutdown('SIGTERM'));
  process.on('SIGINT', () => shutdown('SIGINT'));
}

try {
  main();
} catch (err) {
  // Config errors never contain secrets: they name the variable only.
  console.error(err instanceof Error ? err.message : 'Startup failed');
  process.exit(1);
}
