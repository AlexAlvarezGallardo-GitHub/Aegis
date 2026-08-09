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
import { WalletService } from './wallet.service';
import { DepositReceipt, WalletResponse } from '../../shared/models/wallet.model';
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
  private destroyRef = inject(DestroyRef);

  walletForm: FormGroup;
  isLoading = false;
  isLoadingList = false;
  wallets = signal<WalletResponse[]>([]);
  loadError = signal(false);
  showCreatePanel = signal(false);
  showDetailPanel = signal(false);
  selectedWallet = signal<WalletResponse | null>(null);
  searchQuery = signal('');
  showDepositForm = signal(false);
  isDepositing = signal(false);
  depositSource = signal('');
  depositReference = signal('');
  lastDeposit = signal<DepositReceipt | null>(null);

  readonly fieldLabels: Record<string, string> = {
    currency: 'Currency',
  };

  private lastFocused: HTMLElement | null = null;

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
    if (this.showDetailPanel() || this.showCreatePanel()) {
      this.closeCreatePanel();
      this.closeDetail();
    }
  }

  openCreatePanel(): void {
    this.lastFocused = document.activeElement as HTMLElement | null;
    this.showCreatePanel.set(true);
    this.focusPanel();
  }

  closeCreatePanel(): void {
    this.showCreatePanel.set(false);
    this.walletForm.reset();
    this.lastFocused?.focus();
    this.lastFocused = null;
  }

  openDetail(wallet: WalletResponse): void {
    this.lastFocused = document.activeElement as HTMLElement | null;
    this.selectedWallet.set(wallet);
    this.showDetailPanel.set(true);
    this.focusPanel();
  }

  openDeposit(wallet: WalletResponse): void {
    this.lastFocused = document.activeElement as HTMLElement | null;
    this.selectedWallet.set(wallet);
    this.showDepositForm.set(true);
    this.showDetailPanel.set(true);
    this.focusPanel();
  }

  closeDetail(): void {
    this.showDetailPanel.set(false);
    this.selectedWallet.set(null);
    this.showDepositForm.set(false);
    this.lastFocused?.focus();
    this.lastFocused = null;
  }

  private focusPanel(): void {
    setTimeout(() => {
      const panel = document.querySelector('.slide-panel') as HTMLElement | null;
      panel?.focus();
    });
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
      .pipe(takeUntilDestroyed(this.destroyRef))
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

  depositFunds(amountStr: string): void {
    const wallet = this.selectedWallet();
    if (!wallet) return;

    const amount = parseFloat(amountStr);
    if (isNaN(amount) || amount <= 0) {
      this.toastService.warning('Please enter a valid positive amount');
      return;
    }
    if (!this.depositSource().trim()) {
      this.toastService.warning('Please specify the source of funds');
      return;
    }
    if (!this.depositReference().trim()) {
      this.toastService.warning('Please enter a deposit reference');
      return;
    }

    this.isDepositing.set(true);
    this.lastDeposit.set(null);

    this.walletService.depositFunds(wallet.walletId, {
      amount,
      currency: wallet.currency,
      source: this.depositSource(),
      reference: this.depositReference(),
    }).pipe(
      finalize(() => this.isDepositing.set(false)),
      takeUntilDestroyed(this.destroyRef),
    )
      .subscribe({
        next: (receipt) => {
          this.wallets.update(list =>
            list.map(w => w.walletId === receipt.walletId
              ? { ...w, balance: receipt.newBalance }
              : w)
          );
          this.selectedWallet.update(w => w ? { ...w, balance: receipt.newBalance, updatedAt: receipt.timestamp } : null);
          this.lastDeposit.set(receipt);
          this.depositSource.set('');
          this.depositReference.set('');
          this.showDepositForm.set(false);
          this.toastService.success(`${formatMoney(receipt.amount, receipt.currency)} deposited successfully (ref: ${receipt.reference})`);
        },
        error: (err) => {
          const msg = err.status === 409 ? 'Duplicate deposit reference' : 'Failed to deposit';
          this.toastService.warning(msg);
        },
      });
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
    return formatMoney(balance);
  }

  getBalanceClass(balance: number): string {
    if (balance > 0) return 'balance-positive';
    if (balance < 0) return 'balance-negative';
    return 'balance-zero';
  }
}
