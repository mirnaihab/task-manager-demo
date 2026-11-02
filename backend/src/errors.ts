export type ErrorCode = 'VALIDATION_ERROR' | 'NOT_FOUND' | 'INTERNAL';

export class AppError extends Error {
  constructor(
    readonly code: ErrorCode,
    readonly status: number,
    message: string,
    readonly fields?: Record<string, string>,
  ) {
    super(message);
    this.name = 'AppError';
  }

  static validation(fields: Record<string, string>): AppError {
    return new AppError('VALIDATION_ERROR', 400, 'Invalid request', fields);
  }

  static notFound(message = 'Task not found'): AppError {
    return new AppError('NOT_FOUND', 404, message);
  }
}
