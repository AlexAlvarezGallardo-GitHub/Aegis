import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { AuthComponent } from './auth.component';
import { AuthService } from './auth.service';

describe('AuthComponent', () => {
  let component: AuthComponent;
  let fixture: ComponentFixture<AuthComponent>;

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
    fixture.detectChanges();
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

  it('should have valid form when all fields are filled correctly', () => {
    component.loginForm.patchValue({
      email: 'john@example.com',
      password: 'SecureP@ss1'
    });
    expect(component.loginForm.valid).toBeTruthy();
  });
});
