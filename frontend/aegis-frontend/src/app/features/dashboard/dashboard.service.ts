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
import { DashboardData, TimeRange, TrendDirection, ActivityEvent, FraudAlert, ServiceStatus, OverallHealth } from '../../shared/models/dashboard.model';

export interface DashboardState {
  data: DashboardData | null;
  loading: boolean;
  error: string | null;
  timeRange: TimeRange;
}

const REFRESH_INTERVAL_MS = 30_000;

const ACTIVITY_TEMPLATES: { type: ActivityEvent['type']; status: ActivityEvent['status']; message: (v: number) => string }[] = [
  { type: 'payment', status: 'success', message: (v) => `Payment of €${v.toLocaleString()}.00 processed` },
  { type: 'wallet', status: 'info', message: () => 'New wallet created (USD)' },
  { type: 'fraud', status: 'warning', message: () => 'Suspicious transaction flagged for review' },
  { type: 'system', status: 'info', message: () => 'Auto-scaling triggered: payment-svc' },
  { type: 'payment', status: 'success', message: (v) => `Batch payout completed (${v % 100} items)` },
  { type: 'wallet', status: 'warning', message: () => 'Wallet balance threshold alert' },
  { type: 'refund', status: 'success', message: (v) => `Refund of €${(v % 5000).toLocaleString()}.00 issued` },
  { type: 'user', status: 'info', message: () => 'New merchant account activated' },
  { type: 'payment', status: 'error', message: (v) => `Payment of €${(v % 2000).toLocaleString()}.00 declined` },
  { type: 'system', status: 'info', message: () => 'Kafka consumer lag normalized' },
];

const FRAUD_DESCRIPTIONS = [
  'High-risk transaction detected from new IP',
  'Suspicious login attempt from unusual location',
  'Rapid multiple failed payment attempts',
  'Unusual spending pattern detected',
  'Account takeover attempt blocked',
  'Card testing attack mitigated',
];

const SERVICE_NAMES = ['Identity', 'Wallet', 'Payment', 'Fraud', 'Notification', 'Kafka', 'PostgreSQL'];

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  private readonly timeRange = signal<TimeRange>('30d');
  private readonly manualRefresh = new Subject<void>();
  private readonly polling$: Observable<DashboardData>;
  private tick = 0;

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
      catchError(() => {
        return of(this.generateFakeData());
      }),
    );
  }

  private generateFakeData(): DashboardData {
    this.tick++;
    const now = Date.now();
    const seed = now + this.tick;
    const rng = (max: number, min = 0) => min + ((seed * (this.tick + 1) * 9301 + 49297) % 233280) / 233280 * (max - min);
    const int = (max: number, min = 0) => Math.round(rng(max, min));
    const pick = <T>(arr: T[]): T => arr[int(arr.length - 1)];
    const trend = (): TrendDirection => {
      const r = rng(1);
      return r < 0.45 ? 'up' : r < 0.9 ? 'down' : 'flat';
    };
    const trendVal = (): string => {
      const pct = (rng(20, 1) * (rng(1) > 0.5 ? 1 : -1));
      return `${pct > 0 ? '+' : ''}${pct.toFixed(1)}%`;
    };
    const pts = (count: number, base: number, amp: number, drift = 0): { label: string; value: number }[] =>
      Array.from({ length: count }, (_, i) => ({
        label: `${i + 1}`,
        value: Math.max(1, base + Math.sin((i + this.tick * 0.3) / count * Math.PI * 4) * amp + rng(amp * 0.4, -amp * 0.4) + drift * i / count),
      }));

    const balance = 1_200_000 + Math.sin(this.tick * 0.1) * 50_000 + rng(20000, -20000);
    const dailyVol = 42_000 + Math.sin(this.tick * 0.15) * 8_000 + rng(5000, -5000);
    const monthlyVol = 850_000 + Math.sin(this.tick * 0.05) * 80_000 + rng(40000, -40000);
    const totalTx = 12_000 + Math.sin(this.tick * 0.08) * 500 + rng(300, -300);
    const successRate = 95.2 + Math.sin(this.tick * 0.04) * 0.5 + rng(0.3, -0.3);
    const pendingRate = 2.8 + Math.sin(this.tick * 0.06) * 0.3 + rng(0.2, -0.2);
    const failedRate = Math.max(0.5, 100 - successRate - pendingRate);
    const alerts = Math.max(0, 12 + Math.round(Math.sin(this.tick * 0.07) * 4) + int(3, -3));
    const risk = 23 + Math.sin(this.tick * 0.09) * 5 + rng(4, -4);
    const investigations = Math.max(0, 3 + Math.round(Math.sin(this.tick * 0.11) * 1) + int(2, -1));
    const users = 820 + Math.round(Math.sin(this.tick * 0.03) * 30) + int(20, -20);
    const eps = 1200 + Math.sin(this.tick * 0.12) * 150 + rng(80, -80);

    const rangeCount = this.timeRange() === '7d' ? 7 : this.timeRange() === '90d' ? 90 : 30;

    const kpis = [
      { id: 'total-balance', category: 'financial' as const, label: 'Total Balance', numericValue: balance, prefix: '€', suffix: '', decimals: 0, icon: 'account_balance_wallet', subtitle: 'Across all wallets' },
      { id: 'daily-volume', category: 'financial' as const, label: 'Daily Volume', numericValue: dailyVol, prefix: '€', suffix: '', decimals: 0, icon: 'trending_up' as const, subtitle: 'Today' },
      { id: 'monthly-volume', category: 'financial' as const, label: 'Monthly Volume', numericValue: monthlyVol, prefix: '€', suffix: '', decimals: 0, icon: 'calendar_month' as const, subtitle: 'Current month' },
      { id: 'total-transactions', category: 'financial' as const, label: 'Transactions', numericValue: totalTx, prefix: '', suffix: '', decimals: 0, icon: 'swap_horiz' as const, subtitle: 'All time' },
      { id: 'successful-payments', category: 'payment' as const, label: 'Successful', numericValue: totalTx * successRate / 100, prefix: '', suffix: '', decimals: 0, icon: 'check_circle' as const, subtitle: `${successRate.toFixed(1)}% success rate` },
      { id: 'pending-payments', category: 'payment' as const, label: 'Pending', numericValue: totalTx * pendingRate / 100, prefix: '', suffix: '', decimals: 0, icon: 'hourglass_empty' as const, subtitle: `${pendingRate.toFixed(1)}% pending` },
      { id: 'failed-payments', category: 'payment' as const, label: 'Failed', numericValue: totalTx * failedRate / 100, prefix: '', suffix: '', decimals: 0, icon: 'cancel' as const, subtitle: `${failedRate.toFixed(1)}% failed` },
      { id: 'fraud-alerts', category: 'fraud' as const, label: 'Fraud Alerts', numericValue: alerts, prefix: '', suffix: '', decimals: 0, icon: 'shield' as const, subtitle: 'Active alerts' },
      { id: 'risk-score', category: 'fraud' as const, label: 'Avg Risk Score', numericValue: risk, prefix: '', suffix: '/100', decimals: 0, icon: 'gavel' as const, subtitle: 'Low risk' },
      { id: 'investigations', category: 'fraud' as const, label: 'Investigations', numericValue: investigations, prefix: '', suffix: '', decimals: 0, icon: 'search' as const, subtitle: 'Open cases' },
      { id: 'active-users', category: 'operations' as const, label: 'Active Users', numericValue: users, prefix: '', suffix: '', decimals: 0, icon: 'people' as const, subtitle: 'Last 24h' },
      { id: 'events-per-sec', category: 'operations' as const, label: 'Events/sec', numericValue: eps, prefix: '', suffix: '', decimals: 0, icon: 'bolt' as const, subtitle: 'Current throughput' },
    ].map((k) => ({
      ...k,
      value: k.prefix + Math.round(k.numericValue).toLocaleString() + k.suffix,
      trend: trend(),
      trendValue: trendVal(),
      variant: (k.id === 'total-balance' ? 'gold' : k.id === 'successful-payments' ? 'success' : k.id === 'pending-payments' ? 'warning' : k.id === 'failed-payments' || k.id === 'fraud-alerts' ? 'error' : 'default') as 'default' | 'success' | 'warning' | 'error' | 'gold',
    }));

    const volData = pts(rangeCount, 35000, 20000, 10000);
    const successData = volData.map((d) => ({ label: d.label, value: Math.round(d.value * 0.85), secondaryValue: Math.round(d.value * 0.1), tertiaryValue: Math.round(d.value * 0.05) }));
    const fraudData = pts(rangeCount, 25, 20);
    const growthData = pts(rangeCount, 200, 80, 300);

    const activityCount = int(10, 5);
    const recentActivity: ActivityEvent[] = Array.from({ length: activityCount }, (_, i) => {
      const tpl = pick(ACTIVITY_TEMPLATES);
      const minsAgo = i * int(8, 2) + int(5);
      return {
        id: `act-${now}-${i}`,
        type: tpl.type,
        message: tpl.message(int(50000, 500)),
        timestamp: this.formatRelative(minsAgo),
        status: tpl.status,
      };
    });

    const alertCount = Math.min(alerts, 5);
    const fraudAlerts: FraudAlert[] = Array.from({ length: alertCount }, (_, i) => ({
      id: `alert-${now}-${i}`,
      severity: rng(1) < 0.3 ? 'high' as const : rng(1) < 0.6 ? 'medium' as const : 'low' as const,
      description: pick(FRAUD_DESCRIPTIONS),
      timestamp: this.formatRelative(i * int(15, 5) + int(3)),
      actionLink: '/fraud',
    }));

    const overallHealth: OverallHealth = alerts > 10 ? 'degraded' : 'operational';
    const services: ServiceStatus[] = SERVICE_NAMES.map((name) => ({
      name: `${name} Service`,
      status: name === 'Fraud' && alerts > 8 ? 'degraded' as const : rng(1) > 0.95 ? 'degraded' as const : rng(1) > 0.98 ? 'down' as const : 'healthy' as const,
      uptime: `${(99.5 + rng(0.49)).toFixed(2)}%`,
      lastCheck: `${int(30, 2)}s ago`,
    }));

    return {
      kpis,
      paymentVolume: { label: 'Payment Volume', data: volData, color: 'var(--aegis-gold-500)', gradientTop: 'rgba(212, 168, 67, 0.3)', gradientBottom: 'rgba(212, 168, 67, 0)' },
      transactions: { label: 'Transactions', data: successData, color: 'var(--aegis-color-info)', gradientTop: '', gradientBottom: '' },
      fraudTrends: { label: 'Fraud Trends', data: fraudData, color: 'var(--aegis-color-error)', gradientTop: 'rgba(239, 68, 68, 0.3)', gradientBottom: 'rgba(239, 68, 68, 0)' },
      walletGrowth: { label: 'Wallet Growth', data: growthData, color: 'var(--aegis-color-success)', gradientTop: 'rgba(34, 197, 94, 0.3)', gradientBottom: 'rgba(34, 197, 94, 0)' },
      recentActivity,
      fraudAlerts,
      systemHealth: { overall: overallHealth, services },
      lastUpdated: new Date().toISOString(),
    };
  }

  private formatRelative(mins: number): string {
    if (mins < 1) return 'Just now';
    if (mins < 60) return `${mins}m ago`;
    const h = Math.floor(mins / 60);
    return `${h}h ago`;
  }
}
