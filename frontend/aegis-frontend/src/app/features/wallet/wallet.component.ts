import { Component, OnInit, ChangeDetectionStrategy, inject, DestroyRef, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { WalletService } from './wallet.service';
import { WalletResponse } from '../../shared/models/wallet.model';
import { finalize } from 'rxjs/operators';
import { LoadingButtonComponent } from '../../shared/forms/loading-button/loading-button.component';
import { FormFieldErrorComponent } from '../../shared/forms/form-field-error/form-field-error.component';
import { markFormGroupTouched } from '../../shared/utils/validation.utils';
import { ToastService } from '../../shared/services/toast.service';
import { StatusChipComponent, ChipVariant } from '../../shared/data-display/status-chip/status-chip.component';
import { EmptyStateComponent } from '../../shared/data-display/empty-state/empty-state.component';
import { StatCardComponent } from '../../shared/data-display/stat-card/stat-card.component';

@Component({
  selector: 'app-wallet',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    LoadingButtonComponent,
    FormFieldErrorComponent,
    StatusChipComponent,
    EmptyStateComponent,
    StatCardComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './wallet.component.html',
  styleUrl: './wallet.component.scss',
})
export class WalletComponent implements OnInit {
  private fb = inject(FormBuilder);
  private walletService = inject(WalletService);
  private toastService = inject(ToastService);
  private destroyRef = inject(DestroyRef);

  walletForm: FormGroup;
  isLoading = false;
  isLoadingList = false;
  wallets = signal<WalletResponse[]>([]);
  showCreatePanel = signal(false);
  showDetailPanel = signal(false);
  selectedWallet = signal<WalletResponse | null>(null);
  searchQuery = signal('');

  readonly fieldLabels: Record<string, string> = {
    currency: 'Currency',
  };

  constructor() {
    this.walletForm = this.fb.group({
      currency: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
    });
  }

  ngOnInit(): void {
    this.loadWallets();
  }

  get filteredWallets(): WalletResponse[] {
    const query = this.searchQuery().toLowerCase();
    const all = this.wallets();
    if (!query) return all;
    return all.filter(w =>
      w.currency.toLowerCase().includes(query) ||
      w.walletId.toLowerCase().includes(query)
    );
  }

  get totalBalance(): string {
    const sum = this.wallets().reduce((acc, w) => acc + w.balance, 0);
    return '$' + sum.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  get activeCount(): number {
    return this.wallets().filter(w => w.status.toLowerCase() === 'active').length;
  }

  getCurrenciesCount(): number {
    return new Set(this.wallets().map(w => w.currency)).size;
  }

  loadWallets(): void {
    this.isLoadingList = true;
    this.walletService.getWallets()
      .pipe(
        finalize(() => this.isLoadingList = false),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (wallets) => {
          this.wallets.set(wallets);
        },
        error: () => { /* handled by HttpErrorInterceptor */ },
      });
  }

  openCreatePanel(): void {
    this.showCreatePanel.set(true);
  }

  closeCreatePanel(): void {
    this.showCreatePanel.set(false);
    this.walletForm.reset();
  }

  openDetail(wallet: WalletResponse): void {
    this.selectedWallet.set(wallet);
    this.showDetailPanel.set(true);
  }

  closeDetail(): void {
    this.showDetailPanel.set(false);
    this.selectedWallet.set(null);
  }

  adjustBalance(type: 'DEPOSIT' | 'WITHDRAW', amountStr: string, description: string): void {
    const wallet = this.selectedWallet();
    if (!wallet) return;

    const amount = parseFloat(amountStr);
    if (isNaN(amount) || amount <= 0) {
      this.toastService.warning('Please enter a valid positive amount');
      return;
    }

    const finalAmount = type === 'WITHDRAW' ? -amount : amount;

    this.walletService.adjustBalance(wallet.walletId, finalAmount, description || undefined)
      .subscribe({
        next: (updated) => {
          this.wallets.update(list =>
            list.map(w => w.walletId === updated.walletId ? { ...w, balance: updated.balance, premium: updated.premium, updatedAt: updated.updatedAt } : w)
          );
          this.selectedWallet.set(updated);
          this.toastService.success(`Balance ${type === 'DEPOSIT' ? 'deposited' : 'withdrawn'} successfully`);
        },
        error: () => this.toastService.warning('Failed to adjust balance')
      });
  }

  formatCurrency(balance: number, currency: string): string {
    const prefix = balance < 0 ? '-' : '';
    const abs = Math.abs(balance);
    const symbols: Record<string, string> = { EUR: '€', USD: '$', GBP: '£' };
    return prefix + (symbols[currency] || currency + ' ') + abs.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  onSubmit(): void {
    if (this.walletForm.invalid) {
      markFormGroupTouched(this.walletForm);
      this.toastService.warning('Please fix the form errors before submitting.');
      return;
    }

    this.isLoading = true;

    this.walletService.createWallet({
      currency: this.walletForm.value.currency.toUpperCase(),
    })
      .pipe(
        finalize(() => this.isLoading = false),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (wallet) => {
          this.wallets.update(w => [wallet, ...w]);
          this.walletForm.reset();
          this.showCreatePanel.set(false);
          this.toastService.success(`Wallet created! ID: ${wallet.walletId.slice(0, 8)}...`, 5000);
        },
        error: () => { /* handled by HttpErrorInterceptor */ },
      });
  }

  shortId(id: string): string {
    return id.slice(0, 8) + '...';
  }

  getStatusVariant(status: string): ChipVariant {
    const map: Record<string, ChipVariant> = {
      active: 'success',
      suspended: 'warning',
      closed: 'error',
      pending: 'info',
    };
    return map[status.toLowerCase()] || 'neutral';
  }

  formatBalance(balance: number): string {
    const prefix = balance < 0 ? '-' : '';
    const abs = Math.abs(balance);
    return prefix + '$' + abs.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  getBalanceClass(balance: number): string {
    if (balance > 0) return 'balance-positive';
    if (balance < 0) return 'balance-negative';
    return 'balance-zero';
  }
}
