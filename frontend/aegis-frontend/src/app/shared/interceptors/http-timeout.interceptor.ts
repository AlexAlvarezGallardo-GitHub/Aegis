import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { timeout } from 'rxjs/operators';

const DEFAULT_TIMEOUT_MS = 15_000;

/**
 * Applies a default timeout to all outgoing HTTP requests.
 * Requests that do not complete within the timeout will error with a TimeoutError.
 */
export const httpTimeoutInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> => {
  return next(req).pipe(timeout(DEFAULT_TIMEOUT_MS));
};
