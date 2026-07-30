import { ComponentFixture, TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Routes } from '@angular/router';
import { By } from '@angular/platform-browser';
import { DashboardComponent } from './dashboard.component';
import { DashboardService, DashboardState } from './dashboard.service';
import {
  DashboardData,
  KpiCard,
  ActivityEvent,
  FraudAlert,
  SystemHealth,
} from '../../shared/models/dashboard.model';
import { WritableSignal, signal } from '@angular/core';

// ── Stubs ────────────────────────────────────────────────────────────────────

const stubRoutes: Routes = [{ path: '**', component: class {} }];

function buildKpi(overrides: Partial<KpiCard> = {}): KpiCard {
  return {
    id: 'test-kpi',
    category: 'financial',
    label: 'Test KPI',
    value: '100',
    numericValue: 100,
    prefix: '',
    suffix: '',
    decimals: 0,
    icon: 'test_icon',
    trend: 'up',
    trendValue: '+5%',
    variant: 'default',
    subtitle: '',
    ...overrides,
  };
}

function buildDashboardData(overrides: Partial<DashboardData> = {}): DashboardData {
  return {
    kpis: [
      buildKpi({ id: 'kpi-fin', category: 'financial', label: 'Total Balance', value: '€10,000', numericValue: 10000, prefix: '€', icon: 'account_balance_wallet', variant: 'gold' }),
      buildKpi({ id: 'kpi-pay', category: 'payment', label: 'Payments', value: '500', numericValue: 500, icon: 'payments', variant: 'success' }),
      buildKpi({ id: 'kpi-fraud', category: 'fraud', label: 'Fraud Alerts', value: '3', numericValue: 3, icon: 'shield', variant: 'error' }),
      buildKpi({ id: 'kpi-ops', category: 'operations', label: 'Active Users', value: '120', numericValue: 120, icon: 'people', variant: 'success' }),
    ],
    paymentVolume: {
      label: 'Payment Volume',
      data: [
        { label: 'Mon', value: 120 },
        { label: 'Tue', value: 200 },
        { label: 'Wed', value: 150 },
      ],
      color: 'var(--aegis-gold-500)',
      gradientTop: 'rgba(212, 168, 67, 0.3)',
      gradientBottom: 'rgba(212, 168, 67, 0)',
    },
    transactions: {
      label: 'Transactions',
      data: [{ label: 'Mon', value: 50 }, { label: 'Tue', value: 80 }],
      color: 'var(--aegis-color-info)',
      gradientTop: '',
      gradientBottom: '',
    },
    fraudTrends: {
      label: 'Fraud Trends',
      data: [{ label: 'Mon', value: 10 }],
      color: 'var(--aegis-color-error)',
      gradientTop: 'rgba(239, 68, 68, 0.3)',
      gradientBottom: 'rgba(239, 68, 68, 0)',
    },
    walletGrowth: {
      label: 'Wallet Growth',
      data: [{ label: 'Mon', value: 30 }],
      color: 'var(--aegis-color-success)',
      gradientTop: 'rgba(34, 197, 94, 0.3)',
      gradientBottom: 'rgba(34, 197, 94, 0)',
    },
    recentActivity: [
      { id: 'a1', type: 'payment', message: 'Payment processed', timestamp: '2m ago', status: 'success' },
      { id: 'a2', type: 'fraud', message: 'Suspicious login attempt', timestamp: '5m ago', status: 'warning' },
    ] as ActivityEvent[],
    fraudAlerts: [
      { id: 'f1', severity: 'high', description: 'Suspicious login', timestamp: '5m ago', actionLink: '/fraud/1' },
      { id: 'f2', severity: 'medium', description: 'Unusual pattern', timestamp: '10m ago', actionLink: '/fraud/2' },
      { id: 'f3', severity: 'low', description: 'Minor anomaly', timestamp: '15m ago', actionLink: '' },
    ] as FraudAlert[],
    systemHealth: {
      overall: 'operational',
      services: [
        { name: 'Identity Service', status: 'healthy', uptime: '99.99%', lastCheck: '1m ago' },
        { name: 'Payment Service', status: 'degraded', uptime: '98.50%', lastCheck: '2m ago' },
        { name: 'Fraud Service', status: 'down', uptime: '0%', lastCheck: 'just now' },
      ],
    } as SystemHealth,
    lastUpdated: '2026-07-12T00:00:00Z',
    ...overrides,
  };
}

// ── Mock Service ─────────────────────────────────────────────────────────────

function buildMockState(overrides: Partial<DashboardState> = {}): DashboardState {
  return {
    data: null,
    loading: true,
    error: null,
    timeRange: '30d',
    ...overrides,
  };
}

class MockDashboardService {
  readonly state = signal<DashboardState>(buildMockState());

  setTimeRange = jasmine.createSpy('setTimeRange');
  refresh = jasmine.createSpy('refresh');

  setState(v: DashboardState): void {
    (this.state as WritableSignal<DashboardState>).set(v);
  }
}

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let mockService: MockDashboardService;

  beforeEach(async () => {
    mockService = new MockDashboardService();

    await TestBed.configureTestingModule({
      imports: [
        DashboardComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: DashboardService, useValue: mockService },
        provideRouter(stubRoutes),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  /** Helper: set mock service state and run change detection. */
  function setStateAndDetect(state: Partial<DashboardState>): void {
    mockService.setState(buildMockState(state));
    fixture.detectChanges();
  }

  // ── Creation ─────────────────────────────────────────────────────────────

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  // ── Loading State ────────────────────────────────────────────────────────

  describe('Loading state', () => {
    it('should show loading skeletons when loading and no data', () => {
      setStateAndDetect({ loading: true, data: null, error: null });

      const skeletons = fixture.debugElement.queryAll(By.css('app-loading-skeleton'));
      expect(skeletons.length).toBeGreaterThan(0);
    });

    it('should not show KPI sections while loading', () => {
      setStateAndDetect({ loading: true, data: null, error: null });

      const sections = fixture.debugElement.queryAll(By.css('.kpi-section'));
      expect(sections.length).toBe(0);
    });
  });

  // ── Error State ──────────────────────────────────────────────────────────

  describe('Error state', () => {
    it('should show error state when error occurs and no data', () => {
      setStateAndDetect({ loading: false, data: null, error: 'Network failure' });

      const emptyState = fixture.debugElement.query(By.css('app-empty-state'));
      expect(emptyState).toBeTruthy();
      expect(emptyState.attributes['title']).toContain('Failed to load dashboard');
    });

    it('should not show loading skeletons in error state', () => {
      setStateAndDetect({ loading: false, data: null, error: 'Server error' });

      const skeletons = fixture.debugElement.queryAll(By.css('app-loading-skeleton'));
      expect(skeletons.length).toBe(0);
    });
  });

  // ── Data Rendering ───────────────────────────────────────────────────────

  describe('Data rendering', () => {
    beforeEach(() => {
      setStateAndDetect({ loading: false, data: buildDashboardData(), error: null });
    });

    it('should render KPI categories when data is available', () => {
      const kpiSections = fixture.debugElement.queryAll(By.css('.kpi-section'));
      // 4 categories: financial, payment, fraud, operations
      expect(kpiSections.length).toBe(4);
    });

    it('should render stat cards for each KPI', () => {
      const statCards = fixture.debugElement.queryAll(By.css('app-stat-card'));
      expect(statCards.length).toBe(4);
    });

    it('should render section headers with category titles', () => {
      const titles = fixture.debugElement.queryAll(By.css('.section-title'));
      const titlesText = titles.map((t) => t.nativeElement.textContent.trim());
      expect(titlesText).toContain('Financial');
      expect(titlesText).toContain('Payments');
      expect(titlesText).toContain('Fraud');
      expect(titlesText).toContain('Operations');
    });

    it('should render chart components when chart data exists', () => {
      const chartLines = fixture.debugElement.queryAll(By.css('app-chart-line'));
      const chartBars = fixture.debugElement.queryAll(By.css('app-chart-bar'));
      const chartAreas = fixture.debugElement.queryAll(By.css('app-chart-area'));
      // 2 line charts (paymentVolume, walletGrowth), 1 bar (transactions), 1 area (fraudTrends)
      expect(chartLines.length).toBe(2);
      expect(chartBars.length).toBe(1);
      expect(chartAreas.length).toBe(1);
    });

    it('should render activity rows for recent activity', () => {
      const rows = fixture.debugElement.queryAll(By.css('.activity-row'));
      expect(rows.length).toBe(2);
    });

    it('should render alert rows for fraud alerts', () => {
      const rows = fixture.debugElement.queryAll(By.css('.alert-row'));
      expect(rows.length).toBe(3);
    });

    it('should render system health section', () => {
      const healthItems = fixture.debugElement.queryAll(By.css('.health-item'));
      expect(healthItems.length).toBe(3);
    });

    it('should render page title', () => {
      const title = fixture.debugElement.query(By.css('.page-title'));
      expect(title.nativeElement.textContent).toContain('Dashboard');
    });

    it('should render time range toggle buttons', () => {
      const buttons = fixture.debugElement.queryAll(By.css('.range-btn'));
      expect(buttons.length).toBe(3);
      const labels = buttons.map((b) => b.nativeElement.textContent.trim());
      expect(labels).toEqual(['7d', '30d', '90d']);
    });

    it('should mark the current range button as active', () => {
      const activeBtn = fixture.debugElement.query(By.css('.range-btn.active'));
      expect(activeBtn).toBeTruthy();
      expect(activeBtn.nativeElement.textContent.trim()).toBe('30d');
    });
  });

  // ── Empty States ─────────────────────────────────────────────────────────

  describe('Empty states', () => {
    it('should show empty states when no data sections exist', () => {
      const emptyData = buildDashboardData({
        kpis: [],
        paymentVolume: { label: 'Payment Volume', data: [], color: '', gradientTop: '', gradientBottom: '' },
        transactions: { label: 'Transactions', data: [], color: '', gradientTop: '', gradientBottom: '' },
        fraudTrends: { label: 'Fraud Trends', data: [], color: '', gradientTop: '', gradientBottom: '' },
        walletGrowth: { label: 'Wallet Growth', data: [], color: '', gradientTop: '', gradientBottom: '' },
        recentActivity: [],
        fraudAlerts: [],
        systemHealth: { overall: 'operational', services: [] },
      });
      setStateAndDetect({ loading: false, data: emptyData, error: null });

      // No KPI sections
      expect(fixture.debugElement.queryAll(By.css('.kpi-section')).length).toBe(0);
      // No chart components rendered (data arrays are empty)
      expect(fixture.debugElement.queryAll(By.css('app-chart-line')).length).toBe(0);
      expect(fixture.debugElement.queryAll(By.css('app-chart-bar')).length).toBe(0);
      expect(fixture.debugElement.queryAll(By.css('app-chart-area')).length).toBe(0);
      // Empty state components should appear for activity and alerts
      const emptyStates = fixture.debugElement.queryAll(By.css('app-empty-state'));
      expect(emptyStates.length).toBeGreaterThan(0);
    });
  });

  // ── User Interactions ────────────────────────────────────────────────────

  describe('User interactions', () => {
    beforeEach(() => {
      setStateAndDetect({ loading: false, data: buildDashboardData(), error: null });
    });

    it('should handle time range toggle click', fakeAsync(() => {
      const buttons = fixture.debugElement.queryAll(By.css('.range-btn'));
      const btn7d = buttons.find((b) => b.nativeElement.textContent.trim() === '7d');
      expect(btn7d).toBeTruthy();

      btn7d!.nativeElement.click();
      tick();
      fixture.detectChanges();

      expect(mockService.setTimeRange).toHaveBeenCalledWith('7d');
      expect(component.selectedRange()).toBe('7d');
      flush();
    }));

    it('should handle refresh click', fakeAsync(() => {
      const refreshBtn = fixture.debugElement.query(By.css('button[aria-label="Refresh dashboard"]'));
      expect(refreshBtn).toBeTruthy();

      refreshBtn.nativeElement.click();
      tick();

      expect(mockService.refresh).toHaveBeenCalled();
      expect(component.secondsSinceUpdate()).toBe(0);
      flush();
    }));

    it('should update active range button after toggle', fakeAsync(() => {
      const buttons = fixture.debugElement.queryAll(By.css('.range-btn'));
      const btn90d = buttons.find((b) => b.nativeElement.textContent.trim() === '90d');
      btn90d!.nativeElement.click();
      tick();
      fixture.detectChanges();

      const activeBtn = fixture.debugElement.query(By.css('.range-btn.active'));
      expect(activeBtn.nativeElement.textContent.trim()).toBe('90d');
      flush();
    }));
  });

  // ── Helper Methods ───────────────────────────────────────────────────────

  describe('Helper methods', () => {
    describe('getActivityIcon', () => {
      it('should return correct icon for known activity types', () => {
        expect(component.getActivityIcon('payment')).toBe('payments');
        expect(component.getActivityIcon('wallet')).toBe('account_balance_wallet');
        expect(component.getActivityIcon('fraud')).toBe('shield');
        expect(component.getActivityIcon('system')).toBe('settings');
        expect(component.getActivityIcon('refund')).toBe('undo');
        expect(component.getActivityIcon('user')).toBe('person');
      });

      it('should return fallback icon for unknown type', () => {
        expect(component.getActivityIcon('unknown')).toBe('info');
      });
    });

    describe('getActivityColor', () => {
      it('should return correct color for known activity types', () => {
        expect(component.getActivityColor('payment')).toBe('var(--aegis-color-success)');
        expect(component.getActivityColor('wallet')).toBe('var(--aegis-color-info)');
        expect(component.getActivityColor('fraud')).toBe('var(--aegis-color-error)');
        expect(component.getActivityColor('system')).toBe('var(--aegis-color-text-muted)');
        expect(component.getActivityColor('refund')).toBe('var(--aegis-color-warning)');
        expect(component.getActivityColor('user')).toBe('var(--aegis-color-info)');
      });

      it('should return fallback color for unknown type', () => {
        expect(component.getActivityColor('unknown')).toBe('var(--aegis-color-text-muted)');
      });
    });

    describe('getSeverityIcon', () => {
      it('should return correct icon for severity levels', () => {
        expect(component.getSeverityIcon('high')).toBe('error');
        expect(component.getSeverityIcon('medium')).toBe('warning');
        expect(component.getSeverityIcon('low')).toBe('info');
      });

      it('should return fallback icon for unknown severity', () => {
        expect(component.getSeverityIcon('critical')).toBe('info');
      });
    });

    describe('getSeverityVariant', () => {
      it('should return correct variant for severity levels', () => {
        expect(component.getSeverityVariant('high')).toBe('error');
        expect(component.getSeverityVariant('medium')).toBe('warning');
        expect(component.getSeverityVariant('low')).toBe('info');
      });

      it('should return fallback variant for unknown severity', () => {
        expect(component.getSeverityVariant('critical')).toBe('info');
      });
    });

    describe('getServiceStatusVariant', () => {
      it('should return success for healthy', () => {
        expect(component.getServiceStatusVariant('healthy')).toBe('success');
      });

      it('should return warning for degraded', () => {
        expect(component.getServiceStatusVariant('degraded')).toBe('warning');
      });

      it('should return error for down', () => {
        expect(component.getServiceStatusVariant('down')).toBe('error');
      });

      it('should return neutral for unknown status', () => {
        expect(component.getServiceStatusVariant('unknown')).toBe('neutral');
      });
    });

    describe('getServiceDotClass', () => {
      it('should return dot-success for healthy', () => {
        expect(component.getServiceDotClass('healthy')).toBe('dot-success');
      });

      it('should return dot-warning for degraded', () => {
        expect(component.getServiceDotClass('degraded')).toBe('dot-warning');
      });

      it('should return dot-error for down', () => {
        expect(component.getServiceDotClass('down')).toBe('dot-error');
      });

      it('should return dot-neutral for unknown status', () => {
        expect(component.getServiceDotClass('unknown')).toBe('dot-neutral');
      });
    });

    describe('getOverallStatusLabel', () => {
      it('should return correct label for each overall status', () => {
        expect(component.getOverallStatusLabel('operational')).toBe('All Systems Operational');
        expect(component.getOverallStatusLabel('degraded')).toBe('Degraded Performance');
        expect(component.getOverallStatusLabel('outage')).toBe('Service Outage');
      });

      it('should return Unknown for unrecognized status', () => {
        expect(component.getOverallStatusLabel('something')).toBe('Unknown');
      });
    });

    describe('getOverallStatusVariant', () => {
      it('should return success for operational', () => {
        expect(component.getOverallStatusVariant('operational')).toBe('success');
      });

      it('should return warning for degraded', () => {
        expect(component.getOverallStatusVariant('degraded')).toBe('warning');
      });

      it('should return error for outage', () => {
        expect(component.getOverallStatusVariant('outage')).toBe('error');
      });

      it('should return success as default for unknown status', () => {
        expect(component.getOverallStatusVariant('unknown')).toBe('success');
      });
    });
  });

  // ── Computed Signals ─────────────────────────────────────────────────────

  describe('Computed signals', () => {
    it('should return empty categories when no data', () => {
      setStateAndDetect({ loading: true, data: null, error: null });
      expect(component.kpiCategories()).toEqual([]);
    });

    it('should categorize KPIs into correct groups', () => {
      setStateAndDetect({ loading: false, data: buildDashboardData(), error: null });
      const categories = component.kpiCategories();
      expect(categories.length).toBe(4);
      expect(categories.map((c) => c.title)).toEqual(['Financial', 'Payments', 'Fraud', 'Operations']);
    });

    it('should compute paymentVolume from data', () => {
      setStateAndDetect({ loading: false, data: buildDashboardData(), error: null });
      expect(component.paymentVolume()).toBeTruthy();
      expect(component.paymentVolume()!.data.length).toBe(3);
    });

    it('should return null for chart data when no data available', () => {
      setStateAndDetect({ loading: true, data: null, error: null });
      expect(component.paymentVolume()).toBeNull();
      expect(component.transactions()).toBeNull();
      expect(component.fraudTrends()).toBeNull();
      expect(component.walletGrowth()).toBeNull();
      expect(component.systemHealth()).toBeNull();
    });

    it('should compute recentActivity as empty array when no data', () => {
      setStateAndDetect({ loading: true, data: null, error: null });
      expect(component.recentActivity()).toEqual([]);
    });

    it('should compute hasFraudAlerts correctly', () => {
      setStateAndDetect({ loading: false, data: buildDashboardData(), error: null });
      expect(component.hasFraudAlerts()).toBeTrue();
    });

    it('should compute hasFraudAlerts as false when no alerts', () => {
      const noAlerts = buildDashboardData({ fraudAlerts: [] });
      setStateAndDetect({ loading: false, data: noAlerts, error: null });
      expect(component.hasFraudAlerts()).toBeFalse();
    });
  });
});
