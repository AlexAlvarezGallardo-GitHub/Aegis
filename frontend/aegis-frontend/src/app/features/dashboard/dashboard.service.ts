import { Injectable, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, interval, Subject, merge, of } from 'rxjs';
import {
  catchError,
  map,
  shareReplay,
  switchMap,
  startWith,
} from 'rxjs/operators';
import { DashboardData, TimeRange } from '../../shared/models/dashboard.model';

export interface DashboardState {
  data: DashboardData | null;
  loading: boolean;
  error: string | null;
  timeRange: TimeRange;
}

const REFRESH_INTERVAL_MS = 30_000;

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  private readonly timeRange = signal<TimeRange>('30d');
  private readonly manualRefresh = new Subject<void>();
  private readonly polling$: Observable<DashboardData>;

  readonly state = signal<DashboardState>({
    data: null,
    loading: true,
    error: null,
    timeRange: '30d',
  });

  constructor() {
    this.polling$ = merge(
      toObservable(this.timeRange).pipe(takeUntilDestroyed(this.destroyRef)),
      this.manualRefresh.pipe(takeUntilDestroyed(this.destroyRef)),
      interval(REFRESH_INTERVAL_MS).pipe(takeUntilDestroyed(this.destroyRef)),
    ).pipe(
      startWith(void 0),
      switchMap(() => this.fetchDashboardData(this.timeRange())),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

    this.polling$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) =>
        this.state.set({ data, loading: false, error: null, timeRange: this.timeRange() }),
      error: (err) =>
        this.state.set({
          data: null,
          loading: false,
          error: err instanceof HttpErrorResponse ? err.message : 'Failed to load dashboard data',
          timeRange: this.timeRange(),
        }),
    });
  }

  setTimeRange(range: TimeRange): void {
    this.timeRange.set(range);
    this.state.update((s) => ({ ...s, loading: true }));
  }

  refresh(): void {
    this.manualRefresh.next();
  }

  private fetchDashboardData(range: TimeRange): Observable<DashboardData> {
    return this.http.get<DashboardData>('/api/bff/dashboard', {
      params: { range },
    }).pipe(
      map((data) => ({
        ...data,
        lastUpdated: new Date().toISOString(),
      })),
      catchError((err: HttpErrorResponse) => {
        console.error('[DashboardService] fetch failed', err);
        return of(this.getFallbackData());
      }),
    );
  }

  private getFallbackData(): DashboardData {
    return {
      kpis: [
        { id: 'total-balance', category: 'financial', label: 'Total Balance', value: '€0', numericValue: 0, prefix: '€', suffix: '', decimals: 0, icon: 'account_balance_wallet', trend: 'flat', trendValue: '0%', variant: 'gold', subtitle: '' },
        { id: 'daily-volume', category: 'financial', label: 'Daily Volume', value: '€0', numericValue: 0, prefix: '€', suffix: '', decimals: 0, icon: 'trending_up', trend: 'flat', trendValue: '0%', variant: 'default', subtitle: '' },
        { id: 'monthly-volume', category: 'financial', label: 'Monthly Volume', value: '€0', numericValue: 0, prefix: '€', suffix: '', decimals: 0, icon: 'calendar_month', trend: 'flat', trendValue: '0%', variant: 'default', subtitle: '' },
        { id: 'total-transactions', category: 'financial', label: 'Total Transactions', value: '0', numericValue: 0, prefix: '', suffix: '', decimals: 0, icon: 'swap_horiz', trend: 'flat', trendValue: '0%', variant: 'default', subtitle: '' },
        { id: 'successful-payments', category: 'payment', label: 'Successful Payments', value: '0 (0%)', numericValue: 0, prefix: '', suffix: '', decimals: 0, icon: 'check_circle', trend: 'flat', trendValue: '0%', variant: 'success', subtitle: '' },
        { id: 'pending-payments', category: 'payment', label: 'Pending Payments', value: '0', numericValue: 0, prefix: '', suffix: '', decimals: 0, icon: 'hourglass_empty', trend: 'flat', trendValue: '0%', variant: 'warning', subtitle: '' },
        { id: 'failed-payments', category: 'payment', label: 'Failed Payments', value: '0', numericValue: 0, prefix: '', suffix: '', decimals: 0, icon: 'cancel', trend: 'flat', trendValue: '0%', variant: 'error', subtitle: '' },
        { id: 'fraud-alerts', category: 'fraud', label: 'Fraud Alerts', value: '0', numericValue: 0, prefix: '', suffix: '', decimals: 0, icon: 'shield', trend: 'flat', trendValue: '0%', variant: 'error', subtitle: '' },
        { id: 'risk-score', category: 'fraud', label: 'Avg Risk Score', value: '0/100', numericValue: 0, prefix: '', suffix: '', decimals: 0, icon: 'gavel', trend: 'flat', trendValue: '0%', variant: 'success', subtitle: '' },
        { id: 'investigations', category: 'fraud', label: 'Investigations', value: '0', numericValue: 0, prefix: '', suffix: '', decimals: 0, icon: 'search', trend: 'flat', trendValue: '0%', variant: 'warning', subtitle: '' },
        { id: 'active-users', category: 'operations', label: 'Active Users', value: '0', numericValue: 0, prefix: '', suffix: '', decimals: 0, icon: 'people', trend: 'flat', trendValue: '0%', variant: 'success', subtitle: '' },
        { id: 'events-per-sec', category: 'operations', label: 'Events/sec', value: '0', numericValue: 0, prefix: '', suffix: '', decimals: 0, icon: 'bolt', trend: 'flat', trendValue: '0%', variant: 'default', subtitle: '' },
      ],
      paymentVolume: { label: 'Payment Volume', data: [], color: 'var(--aegis-gold-500)', gradientTop: 'rgba(212, 168, 67, 0.3)', gradientBottom: 'rgba(212, 168, 67, 0)' },
      transactions: { label: 'Transactions', data: [], color: 'var(--aegis-color-info)', gradientTop: '', gradientBottom: '' },
      fraudTrends: { label: 'Fraud Trends', data: [], color: 'var(--aegis-color-error)', gradientTop: 'rgba(239, 68, 68, 0.3)', gradientBottom: 'rgba(239, 68, 68, 0)' },
      walletGrowth: { label: 'Wallet Growth', data: [], color: 'var(--aegis-color-success)', gradientTop: 'rgba(34, 197, 94, 0.3)', gradientBottom: 'rgba(34, 197, 94, 0)' },
      recentActivity: [],
      fraudAlerts: [],
      systemHealth: { overall: 'operational', services: [] },
      lastUpdated: new Date().toISOString(),
    };
  }
}
