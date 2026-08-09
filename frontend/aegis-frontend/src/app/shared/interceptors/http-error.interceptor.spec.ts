import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { httpErrorInterceptor } from './http-error.interceptor';
import { AuthService } from '../../features/auth/auth.service';
import { ToastService } from '../services/toast.service';

describe('httpErrorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let toast: ToastService;
  let router: Router;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
        AuthService,
        ToastService,
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
    router = TestBed.inject(Router);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => httpMock.verify());

  it('should show invalid credentials toast on login 401 without redirect', () => {
    const errorToast = spyOn(toast, 'error');
    const navigate = spyOn(router, 'navigate');
    const setUnauthenticated = spyOn(auth, 'setUnauthenticated');

    http.get('/api/bff/auth/login').subscribe({ error: () => undefined });

    httpMock.expectOne('/api/bff/auth/login').flush(
      { code: 'INVALID_CREDENTIALS', message: 'Invalid email or password.' },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(errorToast).toHaveBeenCalledWith('Invalid email or password.');
    expect(navigate).not.toHaveBeenCalled();
    expect(setUnauthenticated).not.toHaveBeenCalled();
  });

  it('should show session expired toast and redirect on non-login 401', () => {
    const errorToast = spyOn(toast, 'error');
    const navigate = spyOn(router, 'navigate');
    const setUnauthenticated = spyOn(auth, 'setUnauthenticated');

    http.get('/api/bff/wallets').subscribe({ error: () => undefined });

    httpMock.expectOne('/api/bff/wallets').flush(
      { code: 'UNAUTHORIZED' },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(errorToast).toHaveBeenCalledWith('Session expired. Please log in again.');
    expect(setUnauthenticated).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: jasmine.any(String) } });
  });

  it('should surface server message on 400', () => {
    const errorToast = spyOn(toast, 'error');

    http.get('/api/bff/wallets').subscribe({ error: () => undefined });

    httpMock.expectOne('/api/bff/wallets').flush(
      { message: 'Invalid request.' },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(errorToast).toHaveBeenCalledWith('Invalid request.');
  });

  it('should surface server message on 422', () => {
    const errorToast = spyOn(toast, 'error');

    http.get('/api/bff/wallets').subscribe({ error: () => undefined });

    httpMock.expectOne('/api/bff/wallets').flush(
      { message: 'Validation failed.' },
      { status: 422, statusText: 'Unprocessable Entity' },
    );

    expect(errorToast).toHaveBeenCalledWith('Validation failed.');
  });

  it('should show connection message on network error', () => {
    const errorToast = spyOn(toast, 'error');

    http.get('/api/bff/wallets').subscribe({ error: () => undefined });

    httpMock.expectOne('/api/bff/wallets').error(new ProgressEvent('network error'));

    expect(errorToast).toHaveBeenCalledWith('Unable to connect to the server.');
  });

  it('should show server error message on 500', () => {
    const errorToast = spyOn(toast, 'error');

    http.get('/api/bff/wallets').subscribe({ error: () => undefined });

    httpMock.expectOne('/api/bff/wallets').flush(
      { message: 'boom' },
      { status: 500, statusText: 'Internal Server Error' },
    );

    expect(errorToast).toHaveBeenCalledWith('Server error. Please try again later.');
  });

  it('should rethrow the original error', () => {
    const spy = jasmine.createSpy('error');

    http.get('/api/bff/wallets').subscribe({ error: spy });

    httpMock.expectOne('/api/bff/wallets').flush({}, { status: 500, statusText: 'Internal Server Error' });

    expect(spy).toHaveBeenCalledWith(jasmine.any(HttpErrorResponse));
  });
});
