import { describe, expect, it } from 'vitest';
import { loadConfig } from '../src/config';

describe('loadConfig (REQ-015)', () => {
  it('throws a clear message when DATABASE_URL is missing', () => {
    expect(() => loadConfig({})).toThrow('DATABASE_URL is required');
  });
  it('uses default port 3000 and development', () => {
    const config = loadConfig({ DATABASE_URL: 'postgresql://localhost/x' });
    expect(config).toEqual({ databaseUrl: 'postgresql://localhost/x', port: 3000, nodeEnv: 'development' });
  });
  it('rejects a port outside 1 to 65535', () => {
    expect(() => loadConfig({ DATABASE_URL: 'u', PORT: '70000' })).toThrow();
    expect(() => loadConfig({ DATABASE_URL: 'u', PORT: 'abc' })).toThrow();
  });
  it('reads a valid port', () => {
    expect(loadConfig({ DATABASE_URL: 'u', PORT: '8080' }).port).toBe(8080);
  });
});
