import { Component, ChangeDetectionStrategy, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { finalize } from 'rxjs/operators';
import { LoadingButtonComponent } from '../../shared/forms/loading-button/loading-button.component';
import { PasswordInputComponent } from '../../shared/forms/password-input/password-input.component';
import { FormFieldErrorComponent } from '../../shared/forms/form-field-error/form-field-error.component';
import { markFormGroupTouched } from '../../shared/utils/validation.utils';

@Component({
  selector: 'app-auth',
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
    MatDividerModule,
    MatSnackBarModule,
    LoadingButtonComponent,
    PasswordInputComponent,
    FormFieldErrorComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.scss',
})
export class AuthComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  loginForm: FormGroup;
  isLoading = false;

  readonly enableMockLogin = environment.enableMockLogin;

  readonly fieldLabels: Record<string, string> = {
    email: 'Email',
    password: 'Password',
  };

  constructor() {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]],
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      markFormGroupTouched(this.loginForm);
      this.snackBar.open('Please fix the form errors before submitting.', 'Close', {
        duration: 4000,
      });
      return;
    }

    this.isLoading = true;

    this.authService.login(this.loginForm.value)
      .pipe(
        finalize(() => this.isLoading = false),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Login successful!', 'Close', { duration: 2000 });
          setTimeout(() => this.router.navigate(['/wallets']), 500);
        },
        error: (error) => {
          const errorMessage = error.error?.message || 'Login failed. Please try again.';
          this.snackBar.open(errorMessage, 'Close', { duration: 5000 });
        },
      });
  }

  mockLogin(): void {
    this.isLoading = true;
    this.authService.mockLogin()
      .pipe(
        finalize(() => this.isLoading = false),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Mock login successful!', 'Close', { duration: 2000 });
          setTimeout(() => this.router.navigate(['/wallets']), 500);
        },
        error: () => {
          this.snackBar.open('Mock login failed. Is the dev profile active?', 'Close', { duration: 5000 });
        },
      });
  }
}
