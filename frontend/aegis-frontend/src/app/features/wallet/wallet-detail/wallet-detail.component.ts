import { Component, ChangeDetectionStrategy, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Dialog } from '@angular/cdk/dialog';
import { finalize, map } from 'rxjs/operators';
import { WalletService } from '../wallet.service';
import { DepositReceipt, WalletActivity, WalletResponse } from '../../../shared/models/wallet.model';
import { WalletActivityType } from '../../../shared/models/wallet.model';
import { StatusChipComponent, ChipVariant } from '../../../shared/data-display/status-chip/status-chip.component';
import { EmptyStateComponent } from '../../../shared/data-display/empty-state/empty-state.component';
import { LoadingSkeletonComponent } from '../../../shared/data-display/loading-skeleton/loading-skeleton.component';
import { ConfirmationDialogComponent, ConfirmationData } from '../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { ToastService } from '../../../shared/services/toast.service';
import { AegisCurrencyPipe, formatMoney } from '../../../shared/utils/currency.pipe';
import { MoneyDialogComponent, MoneyDialogResult } from './money-dialog.component';

const TABS = ['overview', 'transactions', 'deposits', 'activity', 'audit'] as const;
export type WalletTab = (typeof TABS)[number];

@Component({
  selector: 'app-wallet-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDividerModule,
    MatTabsModule,
    MatTooltipModule,
    AegisCurrencyPipe,
    StatusChipComponent,
    EmptyStateComponent,
    LoadingSkeletonComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './wallet-detail.component.html',
  styleUrl: './wallet-detail.component.scss',
})
export class WalletDetailComponent {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private walletService = inject(WalletService);
  private toastService = inject(ToastService);
  private dialog = inject(Dialog);
  private destroyRef = inject(DestroyRef);

  private readonly walletIdSignal = toSignal(this.route.paramMap.pipe(map((p) => p.get('walletId') ?? '')));

  readonly wallet = signal<WalletResponse | null>(null);
  readonly loading = signal(true);
  readonly loadError = signal(false);

  readonly activeTab = toSignal(
    this.route.queryParams.pipe(map((p) => (TABS.includes(p['tab']) ? (p['tab'] as WalletTab) : 'overview'))),
    { initialValue: 'overview' },
  );

  get tabIndex(): number {
    return TABS.indexOf(this.activeTab() as WalletTab);
  }

  onTabIndexChange(index: number): void {
    const tab = TABS[index];
    if (tab) this.setTab(tab);
  }

  readonly isOperating = signal(false);

  constructor() {
    this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.loadWallet());
  }

  get activities(): WalletActivity[] {
    const walletId = this.wallet()?.walletId;
    return walletId ? this.walletService.getActivitiesFor(walletId) : [];
  }

  loadWallet(): void {
    const walletId = this.walletIdSignal();
    if (!walletId) {
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.loadError.set(false);
    this.walletService.getWallet(walletId)
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (w) => this.wallet.set(w),
        error: () => this.loadError.set(true),
      });
  }

  backToWallets(): void {
    this.router.navigate(['/wallets']);
  }

  setTab(tab: WalletTab): void {
    this.router.navigate([], { queryParams: { tab }, queryParamsHandling: 'merge' });
  }

  goToTab(tab: WalletTab): void {
    this.setTab(tab);
  }

  // ── Financial operations ───────────────────────────────────────────────────

  openDeposit(): void {
    const wallet = this.wallet();
    if (!wallet) return;
    const ref = this.dialog.open<MoneyDialogResult>(MoneyDialogComponent, {
      data: { mode: 'deposit', wallet },
    });
    ref.closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((result) => {
      if (result) this.performDeposit(result, wallet);
    });
  }

  openWithdraw(): void {
    const wallet = this.wallet();
    if (!wallet) return;
    const ref = this.dialog.open<MoneyDialogResult>(MoneyDialogComponent, {
      data: { mode: 'withdraw', wallet },
    });
    ref.closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((result) => {
      if (result) this.performWithdraw(result, wallet);
    });
  }

  private performDeposit(result: MoneyDialogResult, wallet: WalletResponse): void {
    this.isOperating.set(true);
    this.walletService.depositFunds(wallet.walletId, {
      amount: result.amount,
      currency: wallet.currency,
      source: result.source ?? '',
      reference: result.reference ?? '',
    })
      .pipe(
        finalize(() => this.isOperating.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (receipt) => {
          this.applyReceipt(receipt);
          this.walletService.recordActivity({
            walletId: wallet.walletId,
            type: 'DEPOSIT',
            amount: receipt.amount,
            currency: receipt.currency,
            source: receipt.source,
            reference: receipt.reference,
            status: 'COMPLETED',
            timestamp: receipt.timestamp,
          });
          this.toastService.success('Deposit completed', {
            description: `+${formatMoney(receipt.amount, receipt.currency)} · ${receipt.source}`,
            action: { label: 'View transaction', callback: () => this.goToTab('transactions') },
          });
        },
        error: (err) => {
          if (err?.status === 409) {
            this.walletService.recordActivity({
              walletId: wallet.walletId,
              type: 'DEPOSIT',
              amount: result.amount,
              currency: wallet.currency,
              source: result.source ?? '',
              reference: result.reference ?? '',
              status: 'REJECTED',
              timestamp: new Date().toISOString(),
            });
            this.toastService.warning('Unable to complete deposit', { description: 'Duplicate deposit reference.' });
          } else {
            this.toastService.error('Unable to complete deposit', { description: 'Please try again.' });
          }
        },
      });
  }

  private performWithdraw(result: MoneyDialogResult, wallet: WalletResponse): void {
    this.isOperating.set(true);
    this.walletService.adjustBalance(wallet.walletId, -Math.abs(result.amount), result.reason ?? 'Withdrawal')
      .pipe(
        finalize(() => this.isOperating.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (updated) => {
          this.applyWalletUpdate(updated);
          this.walletService.recordActivity({
            walletId: wallet.walletId,
            type: 'WITHDRAWAL',
            amount: -Math.abs(result.amount),
            currency: wallet.currency,
            reference: result.reference,
            status: 'COMPLETED',
            timestamp: new Date().toISOString(),
          });
          this.toastService.success('Withdrawal completed', {
            description: `-${formatMoney(result.amount, wallet.currency)}`,
            action: { label: 'View transaction', callback: () => this.goToTab('transactions') },
          });
        },
        error: () => this.toastService.error('Unable to complete withdrawal', { description: 'Please try again.' }),
      });
  }

  // ── Administrative actions ─────────────────────────────────────────────────

  openAdjustBalance(): void {
    const wallet = this.wallet();
    if (!wallet) return;
    const ref = this.dialog.open<MoneyDialogResult>(MoneyDialogComponent, {
      data: { mode: 'adjust', wallet },
    });
    ref.closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((result) => {
      if (result) this.performAdjustBalance(result, wallet);
    });
  }

  private performAdjustBalance(result: MoneyDialogResult, wallet: WalletResponse): void {
    this.isOperating.set(true);
    this.walletService.adjustBalance(wallet.walletId, result.amount, result.reason)
      .pipe(
        finalize(() => this.isOperating.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (updated) => {
          this.applyWalletUpdate(updated);
          this.walletService.recordActivity({
            walletId: wallet.walletId,
            type: 'ADJUSTMENT',
            amount: result.amount,
            currency: wallet.currency,
            reference: result.reference,
            status: 'COMPLETED',
            timestamp: new Date().toISOString(),
          });
          this.toastService.success('Balance adjusted', { description: formatMoney(result.amount, wallet.currency) });
        },
        error: () => this.toastService.error('Unable to adjust balance', { description: 'Please try again.' }),
      });
  }

  freezeWallet(): void {
    this.confirmAndUpdateStatus('SUSPENDED', 'Freeze Wallet', 'Freezing prevents financial operations on this wallet. Proceed?');
  }

  deactivateWallet(): void {
    this.confirmAndUpdateStatus('CLOSED', 'Deactivate Wallet', 'Deactivating closes this wallet permanently. This is an administrative action. Proceed?');
  }

  private confirmAndUpdateStatus(status: string, title: string, message: string): void {
    const wallet = this.wallet();
    if (!wallet) return;
    const ref = this.dialog.open<boolean>(ConfirmationDialogComponent, {
      data: {
        title,
        message,
        confirmText: 'Confirm',
        cancelText: 'Cancel',
        destructive: status === 'CLOSED',
      } satisfies ConfirmationData,
    });
    ref.closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((confirmed) => {
      if (!confirmed) return;
      this.isOperating.set(true);
      this.walletService.updateStatus(wallet.walletId, status)
        .pipe(
          finalize(() => this.isOperating.set(false)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe({
          next: (updated) => {
            this.applyWalletUpdate(updated);
            this.toastService.success(status === 'CLOSED' ? 'Wallet deactivated' : 'Wallet frozen');
          },
          error: () => this.toastService.error('Unable to update wallet status', { description: 'Please try again.' }),
        });
    });
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private applyReceipt(receipt: DepositReceipt): void {
    this.wallet.update((w) => (w ? { ...w, balance: receipt.newBalance, updatedAt: receipt.timestamp } : w));
  }

  private applyWalletUpdate(updated: WalletResponse): void {
    this.wallet.set(updated);
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

  activityTypeLabel(type: WalletActivityType): string {
    return type === 'DEPOSIT' ? 'Deposit' : type === 'WITHDRAWAL' ? 'Withdrawal' : 'Adjustment';
  }

  activityIcon(type: WalletActivityType): string {
    return type === 'DEPOSIT' ? 'arrow_downward' : type === 'WITHDRAWAL' ? 'arrow_upward' : 'tune';
  }

  shortId(id: string): string {
    return id.length > 16 ? id.slice(0, 16) + '…' : id;
  }
}
