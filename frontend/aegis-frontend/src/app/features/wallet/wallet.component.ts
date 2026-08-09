import { Component, OnInit, ChangeDetectionStrategy, inject, DestroyRef, signal, HostListener } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { A11yModule } from '@angular/cdk/a11y';
import { Router } from '@angular/router';
import { WalletService } from './wallet.service';
import { WalletActivity, WalletResponse } from '../../shared/models/wallet.model';
import { finalize } from 'rxjs/operators';
import { LoadingButtonComponent } from '../../shared/forms/loading-button/loading-button.component';
import { FormFieldErrorComponent } from '../../shared/forms/form-field-error/form-field-error.component';
import { markFormGroupTouched } from '../../shared/utils/validation.utils';
import { ToastService } from '../../shared/services/toast.service';
import { StatusChipComponent, ChipVariant } from '../../shared/data-display/status-chip/status-chip.component';
import { EmptyStateComponent } from '../../shared/data-display/empty-state/empty-state.component';
import { StatCardComponent } from '../../shared/data-display/stat-card/stat-card.component';
import { AegisCurrencyPipe, formatMoney } from '../../shared/utils/currency.pipe';

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
    MatTooltipModule,
    A11yModule,
    AegisCurrencyPipe,
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
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  walletForm: FormGroup;
  isLoading = false;
  isLoadingList = false;
  wallets = signal<WalletResponse[]>([]);
  loadError = signal(false);
  showCreatePanel = signal(false);
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
    return all.filter(w => w.currency.toLowerCase().includes(query));
  }

  totalBalances(): { currency: string; amount: number }[] {
    const byCurrency = new Map<string, number>();
    for (const w of this.wallets()) {
      byCurrency.set(w.currency, (byCurrency.get(w.currency) ?? 0) + w.balance);
    }
    return [...byCurrency.entries()].map(([currency, amount]) => ({ currency, amount }));
  }

  get totalBalanceLabel(): string {
    const totals = this.totalBalances();
    if (totals.length === 0) return '$0.00';
    return totals.map(t => formatMoney(t.amount, t.currency)).join(' · ');
  }

  get activeCount(): number {
    return this.wallets().filter(w => w.status.toLowerCase() === 'active').length;
  }

  getCurrenciesCount(): number {
    return new Set(this.wallets().map(w => w.currency)).size;
  }

  loadWallets(): void {
    this.isLoadingList = true;
    this.loadError.set(false);
    this.walletService.getWallets()
      .pipe(
        finalize(() => this.isLoadingList = false),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (wallets) => {
          this.wallets.set(wallets);
        },
        error: () => {
          this.loadError.set(true);
        },
      });
  }

  @HostListener('window:keydown.escape')
  onEscape(): void {
    if (this.showCreatePanel()) {
      this.closeCreatePanel();
    }
  }

  openCreatePanel(): void {
    this.showCreatePanel.set(true);
  }

  closeCreatePanel(): void {
    this.showCreatePanel.set(false);
    this.walletForm.reset();
  }

  openDetail(wallet: WalletResponse): void {
    this.router.navigate(['/wallets', wallet.walletId]);
  }

  openDeposit(wallet: WalletResponse): void {
    this.router.navigate(['/wallets', wallet.walletId], { queryParams: { tab: 'deposits' } });
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
          this.toastService.success(`${wallet.currency} wallet created successfully`);
        },
        error: () => { /* handled by HttpErrorInterceptor */ },
      });
  }

  getLastActivity(wallet: WalletResponse): WalletActivity | null {
    const acts = this.walletService.getActivitiesFor(wallet.walletId);
    return acts.length > 0 ? acts[0] : null;
  }

  activityLabel(type: string): string {
    return type === 'DEPOSIT' ? 'Deposit' : type === 'WITHDRAWAL' ? 'Withdrawal' : 'Adjustment';
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
    return formatMoney(balance);
  }

  getBalanceClass(balance: number): string {
    if (balance > 0) return 'balance-positive';
    if (balance < 0) return 'balance-negative';
    return 'balance-zero';
  }
}
