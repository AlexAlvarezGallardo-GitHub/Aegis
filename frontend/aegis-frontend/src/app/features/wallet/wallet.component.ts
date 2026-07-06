import { Component, OnInit, ChangeDetectionStrategy, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { WalletService } from './wallet.service';
import { WalletResponse } from '../../shared/models/wallet.model';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-wallet',
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
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTableModule
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './wallet.component.html',
  styleUrl: './wallet.component.scss'
})
export class WalletComponent implements OnInit {
  private fb = inject(FormBuilder);
  private walletService = inject(WalletService);
  private snackBar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);

  walletForm: FormGroup;
  isLoading = false;
  isLoadingList = false;
  wallets: WalletResponse[] = [];
  displayedColumns: string[] = ['currency', 'balance', 'status', 'createdAt', 'walletId'];

  constructor() {
    this.walletForm = this.fb.group({
      currency: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]]
    });
  }

  ngOnInit(): void {
    this.loadWallets();
  }

  loadWallets(): void {
    this.isLoadingList = true;
    this.walletService.getWallets()
      .pipe(
        finalize(() => this.isLoadingList = false),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (wallets) => {
          this.wallets = wallets;
        },
        error: () => {
          this.snackBar.open('Failed to load wallets.', 'Close', { duration: 4000 });
        }
      });
  }

  onSubmit(): void {
    if (this.walletForm.invalid) {
      this.walletForm.markAllAsTouched();
      this.snackBar.open('Please fix the form errors before submitting.', 'Close', {
        duration: 4000
      });
      return;
    }

    this.isLoading = true;

    this.walletService.createWallet({
      currency: this.walletForm.value.currency.toUpperCase()
    })
      .pipe(
        finalize(() => this.isLoading = false),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (wallet) => {
          this.wallets = [wallet, ...this.wallets];
          this.walletForm.reset();
          this.snackBar.open(`Wallet created! ID: ${wallet.walletId.slice(0, 8)}...`, 'Close', {
            duration: 5000
          });
        },
        error: (error) => {
          const errorMessage = error.error?.message || 'Failed to create wallet.';
          this.snackBar.open(errorMessage, 'Close', { duration: 5000 });
        }
      });
  }

  getErrorMessage(controlName: string): string {
    const control = this.walletForm.get(controlName);
    if (control?.hasError('required')) {
      return 'Currency is required';
    }
    if (control?.hasError('minlength') || control?.hasError('maxlength')) {
      return 'Currency must be a 3-letter code (e.g. EUR, USD)';
    }
    return '';
  }

  shortId(id: string): string {
    return id.slice(0, 8) + '...';
  }
}
