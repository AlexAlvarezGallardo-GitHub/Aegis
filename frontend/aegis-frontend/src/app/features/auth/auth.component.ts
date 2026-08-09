import { Component, ChangeDetectionStrategy, inject, DestroyRef, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { finalize } from 'rxjs/operators';
import { LoadingButtonComponent } from '../../shared/forms/loading-button/loading-button.component';
import { PasswordInputComponent } from '../../shared/forms/password-input/password-input.component';
import { FormFieldErrorComponent } from '../../shared/forms/form-field-error/form-field-error.component';
import { markFormGroupTouched } from '../../shared/utils/validation.utils';
import { ToastService } from '../../shared/services/toast.service';

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
  private toastService = inject(ToastService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);

  loginForm: FormGroup;
  isLoading = signal(false);

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
      this.toastService.warning('Please fix the form errors before submitting.');
      return;
    }

    this.isLoading.set(true);

    this.authService.login(this.loginForm.value)
      .pipe(
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.toastService.success('Welcome back, Architect. Loading your financial workspace...', 4000);
          setTimeout(() => this.navigateAfterLogin(), 800);
        },
        error: () => { /* handled by HttpErrorInterceptor */ },
      });
  }

  private navigateAfterLogin(): void {
    const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/wallets';
    this.router.navigateByUrl(returnUrl);
  }

  mockLogin(): void {
    this.isLoading.set(true);
    this.authService.mockLogin()
      .pipe(
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.toastService.success('Welcome back, Architect. Loading your financial workspace...', 4000);
          setTimeout(() => this.navigateAfterLogin(), 800);
        },
        error: () => { /* handled by HttpErrorInterceptor */ },
      });
  }
}
