import { HttpErrorResponse } from '@angular/common/http';
import { AppError, ProblemDetail } from '../models/api-error.model';

const FALLBACK_MESSAGE = 'Não foi possível completar a operação. Tente novamente em instantes.';

// Centraliza a leitura do ProblemDetail do backend (ver ApiExceptionHandler) para que
// toda tela mostre a mesma mensagem de erro para o mesmo problema, sem repetir parsing.
export function toAppError(error: unknown): AppError {
  if (error instanceof HttpErrorResponse) {
    const problem = error.error as ProblemDetail | null;
    const message = problem?.errors?.length
      ? problem.errors.join('; ')
      : (problem?.detail ?? FALLBACK_MESSAGE);
    return { status: error.status, message };
  }
  return { status: 0, message: FALLBACK_MESSAGE };
}
