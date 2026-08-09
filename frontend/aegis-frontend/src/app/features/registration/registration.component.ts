import { Component, ChangeDetectionStrategy, inject, DestroyRef, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RegistrationService } from './registration.service';
import { RegisterUserResponse } from '../../shared/models/registration.model';
import { finalize } from 'rxjs/operators';
import { LoadingButtonComponent } from '../../shared/forms/loading-button/loading-button.component';
import { PasswordInputComponent } from '../../shared/forms/password-input/password-input.component';
import { FormFieldErrorComponent } from '../../shared/forms/form-field-error/form-field-error.component';
import { markFormGroupTouched } from '../../shared/utils/validation.utils';
import { ToastService } from '../../shared/services/toast.service';

@Component({
  selector: 'app-registration',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    LoadingButtonComponent,
    PasswordInputComponent,
    FormFieldErrorComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './registration.component.html',
  styleUrl: './registration.component.scss',
})
export class RegistrationComponent {
  private fb = inject(FormBuilder);
  private registrationService = inject(RegistrationService);
  private toastService = inject(ToastService);
  private destroyRef = inject(DestroyRef);

  registrationForm: FormGroup;
  isLoading = signal(false);
  successResponse = signal<RegisterUserResponse | null>(null);

  readonly fieldLabels: Record<string, string> = {
    email: 'Email',
    password: 'Password',
    firstName: 'First name',
    lastName: 'Last name',
  };

  constructor() {
    this.registrationForm = this.fb.group({
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
    });
  }

  onSubmit(): void {
    if (this.registrationForm.invalid) {
      markFormGroupTouched(this.registrationForm);
      this.toastService.warning('Please fix the form errors before submitting.');
      return;
    }

    this.isLoading.set(true);
    this.successResponse.set(null);

    this.registrationService.register(this.registrationForm.value)
      .pipe(
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          this.successResponse.set(response);
          this.toastService.success('Registration successful! Please check your email.', 5000);
        },
        error: () => { /* handled by HttpErrorInterceptor */ },
      });
  }
}
