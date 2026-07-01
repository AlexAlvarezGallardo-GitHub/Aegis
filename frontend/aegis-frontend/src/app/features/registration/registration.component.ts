import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RegistrationService } from './registration.service';
import { RegisterUserResponse } from '../../shared/models/registration.model';

@Component({
  selector: 'app-registration',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './registration.component.html',
  styleUrl: './registration.component.scss'
})
export class RegistrationComponent {
  private fb = inject(FormBuilder);
  private registrationService = inject(RegistrationService);
  private snackBar = inject(MatSnackBar);

  registrationForm: FormGroup;
  isLoading = false;
  hidePassword = true;
  successResponse: RegisterUserResponse | null = null;

  constructor() {
    this.registrationForm = this.fb.group({
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]]
    });
  }

  onSubmit(): void {
    if (this.registrationForm.invalid) {
      this.registrationForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.successResponse = null;

    this.registrationService.register(this.registrationForm.value).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.successResponse = response;
        this.snackBar.open('Registration successful! Please check your email.', 'Close', {
          duration: 5000
        });
      },
      error: (error) => {
        this.isLoading = false;
        const errorMessage = error.error?.message || 'Registration failed. Please try again.';
        this.snackBar.open(errorMessage, 'Close', {
          duration: 5000
        });
      }
    });
  }

  getErrorMessage(controlName: string): string {
    const control = this.registrationForm.get(controlName);
    if (control?.hasError('required')) {
      return `${this.getFieldLabel(controlName)} is required`;
    }
    if (control?.hasError('email')) {
      return 'Please enter a valid email address';
    }
    if (control?.hasError('minlength')) {
      return `Minimum ${control.errors?.['minlength'].requiredLength} characters required`;
    }
    if (control?.hasError('maxlength')) {
      return `Maximum ${control.errors?.['maxlength'].requiredLength} characters allowed`;
    }
    return '';
  }

  private getFieldLabel(controlName: string): string {
    const labels: Record<string, string> = {
      email: 'Email',
      password: 'Password',
      firstName: 'First name',
      lastName: 'Last name'
    };
    return labels[controlName] || controlName;
  }
}
