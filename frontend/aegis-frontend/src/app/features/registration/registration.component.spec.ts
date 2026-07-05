import { ComponentFixture, TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RegistrationComponent } from './registration.component';
import { RegistrationService } from './registration.service';
import { By } from '@angular/platform-browser';

describe('RegistrationComponent', () => {
  let component: RegistrationComponent;
  let fixture: ComponentFixture<RegistrationComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RegistrationComponent,
        ReactiveFormsModule,
        HttpClientTestingModule,
        NoopAnimationsModule
      ],
      providers: [RegistrationService]
    }).compileComponents();

    fixture = TestBed.createComponent(RegistrationComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have invalid form when empty', () => {
    expect(component.registrationForm.valid).toBeFalsy();
  });

  it('should validate email format', () => {
    component.registrationForm.patchValue({ email: 'invalid-email' });
    expect(component.registrationForm.get('email')?.valid).toBeFalsy();
    component.registrationForm.patchValue({ email: 'valid@example.com' });
    expect(component.registrationForm.get('email')?.valid).toBeTruthy();
  });

  it('should validate password length', () => {
    component.registrationForm.patchValue({ password: 'short' });
    expect(component.registrationForm.get('password')?.valid).toBeFalsy();
    component.registrationForm.patchValue({ password: 'validpassword123' });
    expect(component.registrationForm.get('password')?.valid).toBeTruthy();
  });

  it('should have valid form when all fields filled', () => {
    component.registrationForm.patchValue({
      email: 'john@example.com',
      password: 'validpassword123',
      firstName: 'John',
      lastName: 'Doe'
    });
    expect(component.registrationForm.valid).toBeTruthy();
  });

  it('should disable submit button when form is invalid', () => {
    expect(
      fixture.debugElement.query(By.css('button[type="submit"]')).nativeElement.disabled
    ).toBeTrue();
  });

  it('should disable submit button while loading', () => {
    component.registrationForm.patchValue({
      email: 'john@example.com', password: 'validpassword123', firstName: 'John', lastName: 'Doe'
    });
    component.isLoading = true;
    fixture.detectChanges();
    expect(
      fixture.debugElement.query(By.css('button[type="submit"]')).nativeElement.disabled
    ).toBeTrue();
  });

  it('should enable submit button when form valid and not loading', () => {
    component.registrationForm.patchValue({
      email: 'john@example.com', password: 'validpassword123', firstName: 'John', lastName: 'Doe'
    });
    component.isLoading = false;
    fixture.detectChanges();
    expect(
      fixture.debugElement.query(By.css('button[type="submit"]')).nativeElement.disabled
    ).toBeFalse();
  });

  it('should set isLoading true on submit and reset via finalize', fakeAsync(() => {
    component.registrationForm.patchValue({
      email: 'john@example.com', password: 'validpassword123', firstName: 'John', lastName: 'Doe'
    });
    component.onSubmit();
    expect(component.isLoading).toBeTrue();
    httpMock.expectOne('/api/v1/users/register').flush({
      userId: '123', email: 'john@example.com', status: 'ACTIVE', registeredAt: '2026-01-01'
    });
    tick();
    expect(component.isLoading).toBeFalse();
    flush();
  }));

  it('should reset isLoading via finalize after error', fakeAsync(() => {
    component.registrationForm.patchValue({
      email: 'john@example.com', password: 'validpassword123', firstName: 'John', lastName: 'Doe'
    });
    component.onSubmit();
    expect(component.isLoading).toBeTrue();
    httpMock.expectOne('/api/v1/users/register').flush(
      { code: 'VALIDATION_ERROR', message: 'Email already exists', details: null, timestamp: '2026-01-01' },
      { status: 400, statusText: 'Bad Request' }
    );
    tick();
    expect(component.isLoading).toBeFalse();
    flush();
  }));

  it('should reset isLoading via finalize after network error', fakeAsync(() => {
    component.registrationForm.patchValue({
      email: 'john@example.com', password: 'validpassword123', firstName: 'John', lastName: 'Doe'
    });
    component.onSubmit();
    expect(component.isLoading).toBeTrue();
    httpMock.expectOne('/api/v1/users/register').error(new ProgressEvent('network error'));
    tick();
    expect(component.isLoading).toBeFalse();
    flush();
  }));

  it('should set successResponse and hide form on success', fakeAsync(() => {
    component.registrationForm.patchValue({
      email: 'john@example.com', password: 'validpassword123', firstName: 'John', lastName: 'Doe'
    });
    component.onSubmit();
    httpMock.expectOne('/api/v1/users/register').flush({
      userId: 'abc-123', email: 'john@example.com', status: 'PENDING_VERIFICATION', registeredAt: '2026-01-01T00:00:00Z'
    });
    tick();
    fixture.detectChanges();

    expect(component.successResponse?.userId).toBe('abc-123');
    expect(fixture.debugElement.query(By.css('.success-message'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('form'))).toBeFalsy();
    flush();
  }));

  it('should call relative URL through proxy', fakeAsync(() => {
    component.registrationForm.patchValue({
      email: 'john@example.com', password: 'validpassword123', firstName: 'John', lastName: 'Doe'
    });
    component.onSubmit();
    const req = httpMock.expectOne('/api/v1/users/register');
    expect(req.request.url).toBe('/api/v1/users/register');
    expect(req.request.url).not.toContain('localhost');
    req.flush({ userId: '1', email: 'a@b.com', status: 'ACTIVE', registeredAt: '2026-01-01' });
    tick();
    flush();
  }));
});
