import { z } from 'zod';

const envSchema = z.object({
  DATABASE_URL: z.string({ error: 'DATABASE_URL is required' }).min(1, 'DATABASE_URL is required'),
  PORT: z.coerce.number().int().min(1).max(65535).default(3000),
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
});

export interface Config {
  databaseUrl: string;
  port: number;
  nodeEnv: 'development' | 'test' | 'production';
}

export function loadConfig(env: NodeJS.ProcessEnv): Config {
  const result = envSchema.safeParse(env);
  if (!result.success) {
    const problems = result.error.issues.map((i) => `${i.path.join('.')}: ${i.message}`);
    throw new Error(`Invalid configuration. ${problems.join('. ')}`);
  }
  const { DATABASE_URL, PORT, NODE_ENV } = result.data;
  return { databaseUrl: DATABASE_URL, port: PORT, nodeEnv: NODE_ENV };
}
