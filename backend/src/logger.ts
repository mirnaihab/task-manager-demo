import pino from 'pino';

export const logger = pino({
  level: process.env['NODE_ENV'] === 'test' ? 'silent' : 'info',
  base: { service: 'task-manager-backend' },
  messageKey: 'message',
  formatters: { level: (label) => ({ level: label }) },
  timestamp: pino.stdTimeFunctions.isoTime,
});
