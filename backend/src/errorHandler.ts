import type { ErrorRequestHandler, RequestHandler } from 'express';
import { ZodError } from 'zod';
import { AppError } from './errors';
import { logger } from './logger';

interface ErrorBody {
  error: { code: string; message: string; fields?: Record<string, string> };
}

function send(res: Parameters<ErrorRequestHandler>[2], status: number, error: ErrorBody['error']): void {
  res.status(status).json({ error } satisfies ErrorBody);
}

function zodFields(error: ZodError): Record<string, string> {
  const fields: Record<string, string> = {};
  for (const issue of error.issues) {
    const key = issue.path.length > 0 ? issue.path.join('.') : '_';
    fields[key] ??= issue.message;
  }
  return fields;
}

function bodyParserType(err: unknown): string | undefined {
  if (typeof err === 'object' && err !== null && 'type' in err && typeof err.type === 'string') {
    return err.type;
  }
  return undefined;
}

export const notFoundHandler: RequestHandler = (_req, res) => {
  send(res, 404, { code: 'NOT_FOUND', message: 'Route not found' });
};

// Express identifies error middleware by its four arguments.
// eslint-disable-next-line @typescript-eslint/no-unused-vars
export const errorHandler: ErrorRequestHandler = (err, req, res, _next) => {
  if (err instanceof AppError) {
    send(res, err.status, {
      code: err.code,
      message: err.message,
      ...(err.fields ? { fields: err.fields } : {}),
    });
    return;
  }
  if (err instanceof ZodError) {
    send(res, 400, { code: 'VALIDATION_ERROR', message: 'Invalid request', fields: zodFields(err) });
    return;
  }
  const parserType = bodyParserType(err);
  if (parserType === 'entity.parse.failed' || parserType === 'entity.too.large') {
    const message = parserType === 'entity.too.large' ? 'Request body is too large' : 'Request body is not valid JSON';
    send(res, 400, { code: 'VALIDATION_ERROR', message });
    return;
  }
  logger.error({ err, method: req.method, path: req.path }, 'Request failed with unexpected error');
  send(res, 500, { code: 'INTERNAL', message: 'Something went wrong' });
};
