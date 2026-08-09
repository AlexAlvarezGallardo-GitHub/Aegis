import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthService } from '../../features/auth/auth.service';
import { ToastService } from '../services/toast.service';

export const httpErrorInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> => {
  const toastService = inject(ToastService);
  const router = inject(Router);
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let message = 'An unexpected error occurred.';

      if (error.error instanceof ErrorEvent) {
        message = 'Network error. Please check your connection.';
      } else {
        const isLoginRequest = req.url.endsWith('/auth/login') || req.url.endsWith('/auth/mock-login');
        switch (error.status) {
          case 0:
            message = 'Unable to connect to the server.';
            break;
          case 400:
            message = error.error?.message || 'Invalid request.';
            break;
          case 401:
            if (isLoginRequest) {
              message = error.error?.message || 'Invalid email or password.';
            } else {
              message = 'Session expired. Please log in again.';
              authService.setUnauthenticated();
              router.navigate(['/login'], {
                queryParams: { returnUrl: router.url },
              });
            }
            break;
          case 403:
            message = 'You do not have permission to perform this action.';
            break;
          case 404:
            message = 'The requested resource was not found.';
            break;
          case 409:
            message = error.error?.message || 'A conflict occurred.';
            break;
          case 422:
            message = error.error?.message || 'Validation failed.';
            break;
          case 500:
            message = 'Server error. Please try again later.';
            break;
          default:
            message = error.error?.message || `Error ${error.status}.`;
        }
      }

      toastService.error(message);

      return throwError(() => error);
    }),
  );
};
