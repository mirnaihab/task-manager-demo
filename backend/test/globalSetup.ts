import { execFileSync } from 'node:child_process';

const DEFAULT_TEST_URL = 'postgresql://localhost:5432/taskmanager_test';

export default function setup(): void {
  const url = process.env['TEST_DATABASE_URL'] ?? DEFAULT_TEST_URL;
  // Protects the dev database: tests truncate the tasks table.
  if (!new URL(url).pathname.endsWith('_test')) {
    throw new Error('Refusing to run tests: the database name must end in _test');
  }
  execFileSync('npx', ['node-pg-migrate', '-m', 'migrations', '--migration-file-language', 'sql', 'up'], {
    env: { ...process.env, DATABASE_URL: url },
    stdio: 'pipe',
  });
}
