export type TimeRange = '7d' | '30d' | '90d';

export type TrendDirection = 'up' | 'down' | 'flat';

export type StatVariant = 'default' | 'success' | 'warning' | 'error' | 'gold';

export type ActivityType = 'payment' | 'wallet' | 'fraud' | 'system' | 'refund' | 'user';

export type Severity = 'high' | 'medium' | 'low';

export type ServiceHealth = 'healthy' | 'degraded' | 'down';

export type OverallHealth = 'operational' | 'degraded' | 'outage';

export interface KpiCard {
  id: string;
  category: 'financial' | 'payment' | 'fraud' | 'operations';
  label: string;
  value: string;
  numericValue: number;
  prefix: string;
  suffix: string;
  decimals: number;
  icon: string;
  trend: TrendDirection;
  trendValue: string;
  variant: StatVariant;
  subtitle: string;
}

export interface ChartDataPoint {
  label: string;
  value: number;
  secondaryValue?: number;
  tertiaryValue?: number;
}

export interface ChartDataset {
  label: string;
  data: ChartDataPoint[];
  color: string;
  gradientTop: string;
  gradientBottom: string;
}

export interface ActivityEvent {
  id: string;
  type: ActivityType;
  message: string;
  timestamp: string;
  status: 'success' | 'warning' | 'error' | 'info';
}

export interface FraudAlert {
  id: string;
  severity: Severity;
  description: string;
  timestamp: string;
  actionLink: string;
}

export interface ServiceStatus {
  name: string;
  status: ServiceHealth;
  uptime: string;
  lastCheck: string;
}

export interface SystemHealth {
  overall: OverallHealth;
  services: ServiceStatus[];
}

export interface DashboardData {
  kpis: KpiCard[];
  paymentVolume: ChartDataset;
  transactions: ChartDataset;
  fraudTrends: ChartDataset;
  walletGrowth: ChartDataset;
  recentActivity: ActivityEvent[];
  fraudAlerts: FraudAlert[];
  systemHealth: SystemHealth;
  lastUpdated: string;
}
