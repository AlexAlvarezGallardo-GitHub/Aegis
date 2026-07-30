import { Component, ChangeDetectionStrategy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { StatCardComponent } from '../../shared/data-display/stat-card/stat-card.component';
import { StatusChipComponent } from '../../shared/data-display/status-chip/status-chip.component';

interface SystemStatus {
  name: string;
  status: 'healthy' | 'degraded' | 'down';
  latency: string;
}

interface ActivityEvent {
  type: 'payment' | 'wallet' | 'fraud' | 'system';
  message: string;
  time: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatIconModule, StatCardComponent, StatusChipComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  readonly kpis: {
    label: string;
    value: string;
    icon: string;
    trend: 'up' | 'down' | 'flat';
    trendValue: string;
    variant: 'default' | 'success' | 'warning' | 'error' | 'gold';
    animate: boolean;
    countUpValue: number;
    countUpPrefix: string;
    countUpDecimals: number;
  }[] = [
    { label: 'Total Balance', value: '$2,847,392', icon: 'account_balance_wallet', trend: 'up', trendValue: '+12.3%', variant: 'gold', animate: true, countUpValue: 2847392, countUpPrefix: '$', countUpDecimals: 0 },
    { label: 'Transactions Today', value: '12,847', icon: 'swap_horiz', trend: 'up', trendValue: '+12.3%', variant: 'default', animate: true, countUpValue: 12847, countUpPrefix: '', countUpDecimals: 0 },
    { label: 'Active Wallets', value: '89,234', icon: 'wallet', trend: 'up', trendValue: '+5.7%', variant: 'default', animate: true, countUpValue: 89234, countUpPrefix: '', countUpDecimals: 0 },
    { label: 'Fraud Alerts', value: '3', icon: 'shield', trend: 'down', trendValue: '-28.4%', variant: 'error', animate: false, countUpValue: 0, countUpPrefix: '', countUpDecimals: 0 },
    { label: 'API Latency', value: '42ms', icon: 'speed', trend: 'flat', trendValue: '0.0%', variant: 'success', animate: false, countUpValue: 0, countUpPrefix: '', countUpDecimals: 0 },
    { label: 'Success Rate', value: '99.97%', icon: 'check_circle', trend: 'up', trendValue: '+0.02%', variant: 'success', animate: false, countUpValue: 0, countUpPrefix: '', countUpDecimals: 0 },
  ];

  readonly systemStatuses: SystemStatus[] = [
    { name: 'Identity Service', status: 'healthy', latency: '12ms' },
    { name: 'Wallet Service', status: 'healthy', latency: '8ms' },
    { name: 'Payment Service', status: 'healthy', latency: '15ms' },
    { name: 'Fraud Detection', status: 'degraded', latency: '142ms' },
    { name: 'Notification Service', status: 'healthy', latency: '6ms' },
    { name: 'Kafka Cluster', status: 'healthy', latency: '3ms' },
  ];

  readonly recentActivity: ActivityEvent[] = [
    { type: 'payment', message: 'Payment of $12,400.00 processed', time: '2m ago' },
    { type: 'wallet', message: 'New wallet created (USD)', time: '5m ago' },
    { type: 'fraud', message: 'Suspicious transaction flagged', time: '8m ago' },
    { type: 'system', message: 'Auto-scaling triggered: payment-svc', time: '12m ago' },
    { type: 'payment', message: 'Batch payout completed (47 items)', time: '15m ago' },
    { type: 'wallet', message: 'Wallet balance threshold alert', time: '18m ago' },
    { type: 'payment', message: 'Payment of $3,200.00 processed', time: '22m ago' },
    { type: 'system', message: 'Kafka consumer lag normalized', time: '25m ago' },
  ];

  readonly chartData = signal<number[]>([35, 42, 38, 55, 48, 62, 58, 72, 68, 85, 78, 92, 88, 95, 82, 90, 96, 88, 94, 100, 92, 98, 105, 95, 110, 102, 115, 108, 120, 112]);

  readonly barData = signal<number[]>([45, 52, 48, 65, 58, 72, 68, 82, 75, 90, 85, 95]);

  readonly miniLineData = signal<number[]>([20, 25, 22, 30, 28, 35, 32, 38, 36, 42, 40, 45, 43, 48, 46, 50, 48, 52, 50, 55]);

  getStatusClass(status: string): string {
    switch (status) {
      case 'healthy': return 'success';
      case 'degraded': return 'warning';
      case 'down': return 'error';
      default: return 'neutral';
    }
  }

  getActivityIcon(type: string): string {
    switch (type) {
      case 'payment': return 'payments';
      case 'wallet': return 'account_balance_wallet';
      case 'fraud': return 'shield';
      case 'system': return 'settings';
      default: return 'info';
    }
  }

  getActivityColor(type: string): string {
    switch (type) {
      case 'payment': return 'var(--aegis-color-success)';
      case 'wallet': return 'var(--aegis-color-info)';
      case 'fraud': return 'var(--aegis-color-error)';
      case 'system': return 'var(--aegis-color-text-muted)';
      default: return 'var(--aegis-color-text-muted)';
    }
  }

  getLinePath(): string {
    const data = this.miniLineData();
    const max = Math.max(...data);
    const width = 200;
    const height = 60;
    const step = width / (data.length - 1);

    const points = data.map((val, i) => {
      const x = i * step;
      const y = height - (val / max) * (height - 4) - 2;
      return `${x},${y}`;
    });

    const linePath = `M${points.join(' L')}`;
    const areaPath = `${linePath} L${width},${height} L0,${height} Z`;
    return areaPath;
  }
}
