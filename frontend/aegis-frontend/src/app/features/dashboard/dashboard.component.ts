import { Component, ChangeDetectionStrategy, inject, computed, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { interval } from 'rxjs';
import { StatCardComponent } from '../../shared/data-display/stat-card/stat-card.component';
import { StatusChipComponent } from '../../shared/data-display/status-chip/status-chip.component';
import { LoadingSkeletonComponent } from '../../shared/data-display/loading-skeleton/loading-skeleton.component';
import { EmptyStateComponent } from '../../shared/data-display/empty-state/empty-state.component';
import { ChartLineComponent } from '../../shared/data-display/chart-line/chart-line.component';
import { ChartBarComponent } from '../../shared/data-display/chart-bar/chart-bar.component';
import { ChartAreaComponent } from '../../shared/data-display/chart-area/chart-area.component';
import { DashboardService } from './dashboard.service';
import { TimeRange, KpiCard } from '../../shared/models/dashboard.model';

const SEVERITY_ICONS: Record<string, string> = { high: 'error', medium: 'warning', low: 'info' };
const SEVERITY_VARIANTS: Record<string, 'error' | 'warning' | 'info'> = { high: 'error', medium: 'warning', low: 'info' };
const ACTIVITY_ICONS: Record<string, string> = {
  payment: 'payments', wallet: 'account_balance_wallet', fraud: 'shield',
  system: 'settings', refund: 'undo', user: 'person',
};
const ACTIVITY_COLORS: Record<string, string> = {
  payment: 'var(--aegis-color-success)', wallet: 'var(--aegis-color-info)',
  fraud: 'var(--aegis-color-error)', system: 'var(--aegis-color-text-muted)',
  refund: 'var(--aegis-color-warning)', user: 'var(--aegis-color-info)',
};

interface KpiCategory {
  title: string;
  icon: string;
  cards: KpiCard[];
}

function categorizeKpis(kpis: KpiCard[]): KpiCategory[] {
  const map: Record<string, { title: string; icon: string }> = {
    financial: { title: 'Financial', icon: 'account_balance' },
    payment: { title: 'Payments', icon: 'payments' },
    fraud: { title: 'Fraud', icon: 'shield' },
    operations: { title: 'Operations', icon: 'settings' },
  };
  const groups: Record<string, KpiCard[]> = { financial: [], payment: [], fraud: [], operations: [] };
  for (const kpi of kpis) groups[kpi.category]?.push(kpi);
  return Object.entries(groups)
    .filter(([, cards]) => cards.length > 0)
    .map(([key, cards]) => ({ ...map[key], cards }));
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatIconModule, MatButtonModule,
    StatCardComponent, StatusChipComponent, LoadingSkeletonComponent, EmptyStateComponent,
    ChartLineComponent, ChartBarComponent, ChartAreaComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  private readonly dashboardService = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);

  readonly data = this.dashboardService.state.asReadonly();
  readonly selectedRange = signal<TimeRange>('30d');

  readonly kpiCategories = computed<KpiCategory[]>(() => {
    const d = this.data().data;
    return d ? categorizeKpis(d.kpis) : [];
  });

  readonly paymentVolume = computed(() => this.data().data?.paymentVolume ?? null);
  readonly transactions = computed(() => this.data().data?.transactions ?? null);
  readonly fraudTrends = computed(() => this.data().data?.fraudTrends ?? null);
  readonly walletGrowth = computed(() => this.data().data?.walletGrowth ?? null);
  readonly recentActivity = computed(() => this.data().data?.recentActivity ?? []);
  readonly fraudAlerts = computed(() => this.data().data?.fraudAlerts ?? []);
  readonly systemHealth = computed(() => this.data().data?.systemHealth ?? null);
  readonly lastUpdated = computed(() => this.data().data?.lastUpdated ?? '');
  readonly hasFraudAlerts = computed(() => this.fraudAlerts().length > 0);

  readonly secondsSinceUpdate = signal(0);

  constructor() {
    interval(1000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.secondsSinceUpdate.update((s) => s + 1);
    });
  }

  setRange(range: TimeRange): void {
    this.selectedRange.set(range);
    this.dashboardService.setTimeRange(range);
  }

  refresh(): void {
    this.secondsSinceUpdate.set(0);
    this.dashboardService.refresh();
  }

  getActivityIcon(type: string): string {
    return ACTIVITY_ICONS[type] ?? 'info';
  }

  getActivityColor(type: string): string {
    return ACTIVITY_COLORS[type] ?? 'var(--aegis-color-text-muted)';
  }

  getSeverityIcon(severity: string): string {
    return SEVERITY_ICONS[severity] ?? 'info';
  }

  getSeverityVariant(severity: string): 'error' | 'warning' | 'info' {
    return SEVERITY_VARIANTS[severity] ?? 'info';
  }

  getServiceStatusVariant(status: string): 'success' | 'warning' | 'error' | 'neutral' {
    switch (status) {
      case 'healthy': return 'success';
      case 'degraded': return 'warning';
      case 'down': return 'error';
      default: return 'neutral';
    }
  }

  getServiceDotClass(status: string): string {
    switch (status) {
      case 'healthy': return 'dot-success';
      case 'degraded': return 'dot-warning';
      case 'down': return 'dot-error';
      default: return 'dot-neutral';
    }
  }

  getOverallStatusLabel(overall: string): string {
    switch (overall) {
      case 'operational': return 'All Systems Operational';
      case 'degraded': return 'Degraded Performance';
      case 'outage': return 'Service Outage';
      default: return 'Unknown';
    }
  }

  getOverallStatusVariant(overall: string): 'success' | 'warning' | 'error' {
    switch (overall) {
      case 'operational': return 'success';
      case 'degraded': return 'warning';
      case 'outage': return 'error';
      default: return 'success';
    }
  }
}
