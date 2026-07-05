import { ComponentFixture, TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { AuthComponent } from './auth.component';
import { AuthService } from './auth.service';

describe('AuthComponent', () => {
  let component: AuthComponent;
  let fixture: ComponentFixture<AuthComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        AuthComponent,
        ReactiveFormsModule,
        HttpClientTestingModule,
        NoopAnimationsModule
      ],
      providers: [AuthService, provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(AuthComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have invalid form when empty', () => {
    expect(component.loginForm.valid).toBeFalsy();
  });

  it('should validate email format', () => {
    component.loginForm.patchValue({ email: 'invalid-email', password: 'password' });
    expect(component.loginForm.get('email')?.valid).toBeFalsy();
    component.loginForm.patchValue({ email: 'valid@example.com', password: 'password' });
    expect(component.loginForm.get('email')?.valid).toBeTruthy();
  });

  it('should have valid form when all fields filled', () => {
    component.loginForm.patchValue({ email: 'john@example.com', password: 'SecureP@ss1' });
    expect(component.loginForm.valid).toBeTruthy();
  });

  it('should disable submit button when form is invalid', () => {
    expect(
      fixture.debugElement.query(By.css('button[type="submit"]')).nativeElement.disabled
    ).toBeTrue();
  });

  it('should disable submit button while loading', () => {
    component.loginForm.patchValue({ email: 'john@example.com', password: 'password' });
    component.isLoading = true;
    fixture.detectChanges();
    expect(
      fixture.debugElement.query(By.css('button[type="submit"]')).nativeElement.disabled
    ).toBeTrue();
  });

  it('should enable submit button when form valid and not loading', () => {
    component.loginForm.patchValue({ email: 'john@example.com', password: 'password' });
    component.isLoading = false;
    fixture.detectChanges();
    expect(
      fixture.debugElement.query(By.css('button[type="submit"]')).nativeElement.disabled
    ).toBeFalse();
  });

  it('should set isLoading and reset via finalize on success', fakeAsync(() => {
    component.loginForm.patchValue({ email: 'john@example.com', password: 'password' });
    component.onSubmit();
    expect(component.isLoading).toBeTrue();
    httpMock.expectOne('/api/bff/auth/login').flush({
      tokenType: 'Bearer', expiresIn: 900, emailVerified: true
    });
    tick();
    expect(component.isLoading).toBeFalse();
    flush();
  }));

  it('should reset isLoading via finalize on error', fakeAsync(() => {
    component.loginForm.patchValue({ email: 'john@example.com', password: 'wrong' });
    component.onSubmit();
    expect(component.isLoading).toBeTrue();
    httpMock.expectOne('/api/bff/auth/login').flush(
      { code: 'INVALID_CREDENTIALS', message: 'Invalid email or password.', details: null, timestamp: '2026-01-01T00:00:00Z' },
      { status: 401, statusText: 'Unauthorized' }
    );
    tick();
    expect(component.isLoading).toBeFalse();
    flush();
  }));

  it('should reset isLoading via finalize on network error', fakeAsync(() => {
    component.loginForm.patchValue({ email: 'john@example.com', password: 'password' });
    component.onSubmit();
    expect(component.isLoading).toBeTrue();
    httpMock.expectOne('/api/bff/auth/login').error(new ProgressEvent('network error'));
    tick();
    expect(component.isLoading).toBeFalse();
    flush();
  }));

  it('should NOT store tokens in localStorage (BFF uses HttpOnly cookies)', fakeAsync(() => {
    component.loginForm.patchValue({ email: 'john@example.com', password: 'password' });
    component.onSubmit();
    httpMock.expectOne('/api/bff/auth/login').flush({
      tokenType: 'Bearer', expiresIn: 900, emailVerified: true
    });
    tick();
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
    flush();
  }));

  it('should call relative URL through proxy', fakeAsync(() => {
    component.loginForm.patchValue({ email: 'john@example.com', password: 'password' });
    component.onSubmit();
    const req = httpMock.expectOne('/api/bff/auth/login');
    expect(req.request.url).toBe('/api/bff/auth/login');
    expect(req.request.url).not.toContain('localhost');
    req.flush({ tokenType: 'Bearer', expiresIn: 900, emailVerified: true });
    tick();
    flush();
  }));
});
