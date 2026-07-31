import { TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { httpTimeoutInterceptor } from './http-timeout.interceptor';

describe('httpTimeoutInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpTimeoutInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should allow requests that complete within the timeout', fakeAsync(() => {
    let result: unknown = null;
    http.get('/api/test').subscribe((data) => (result = data));

    httpMock.expectOne('/api/test').flush({ ok: true });
    tick();

    expect(result).toEqual({ ok: true });
    flush();
  }));

  it('should error when request exceeds the 15s timeout', fakeAsync(() => {
    let error: unknown = null;
    http.get('/api/slow').subscribe({
      error: (err) => (error = err),
    });

    // Advance past the 15s timeout
    tick(15_001);

    expect(error).toBeTruthy();
    httpMock.expectOne('/api/slow');
    flush();
  }));
});
