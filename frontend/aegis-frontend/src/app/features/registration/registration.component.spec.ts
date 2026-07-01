import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RegistrationComponent } from './registration.component';
import { RegistrationService } from './registration.service';

describe('RegistrationComponent', () => {
  let component: RegistrationComponent;
  let fixture: ComponentFixture<RegistrationComponent>;

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
    fixture.detectChanges();
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

  it('should have valid form when all fields are filled correctly', () => {
    component.registrationForm.patchValue({
      email: 'john@example.com',
      password: 'validpassword123',
      firstName: 'John',
      lastName: 'Doe'
    });
    expect(component.registrationForm.valid).toBeTruthy();
  });
});
