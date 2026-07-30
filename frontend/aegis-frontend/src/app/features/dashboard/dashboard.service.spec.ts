import { TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DashboardService } from './dashboard.service';
import { DashboardData } from '../../shared/models/dashboard.model';

/**
 * Builds a minimal valid DashboardData payload for testing.
 */
function buildDashboardData(overrides: Partial<DashboardData> = {}): DashboardData {
  return {
    kpis: [
      {
        id: 'total-balance', category: 'financial', label: 'Total Balance',
        value: '€10,000', numericValue: 10000, prefix: '€', suffix: '',
        decimals: 0, icon: 'account_balance_wallet', trend: 'up',
        trendValue: '+5.2%', variant: 'gold', subtitle: 'Across all wallets',
      },
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
      data: [{ label: 'Mon', value: 50 }],
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
    ],
    fraudAlerts: [
      { id: 'f1', severity: 'high', description: 'Suspicious login', timestamp: '5m ago', actionLink: '/fraud/1' },
    ],
    systemHealth: {
      overall: 'operational',
      services: [
        { name: 'Identity Service', status: 'healthy', uptime: '99.99%', lastCheck: '1m ago' },
      ],
    },
    lastUpdated: '2026-07-12T00:00:00Z',
    ...overrides,
  };
}

/** Match function: request targets the dashboard endpoint (ignoring query params). */
const isDashboardRequest = (r: { url: string }) => r.url === '/api/bff/dashboard';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DashboardService],
    });
    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  /**
   * Drains the initial HTTP request that the service fires on construction
   * via the polling$ stream (startWith(void 0)).
   */
  function flushInitialRequest(data?: DashboardData): void {
    const req = httpMock.expectOne(isDashboardRequest);
    req.flush(data ?? buildDashboardData());
  }

  it('should be created', () => {
    flushInitialRequest();
    expect(service).toBeTruthy();
  });

  it('should start in loading state with null data', () => {
    const state = service.state();
    expect(state.loading).toBeTrue();
    expect(state.data).toBeNull();
    expect(state.error).toBeNull();
    expect(state.timeRange).toBe('30d');
    flushInitialRequest();
  });

  it('should fetch dashboard data from /api/bff/dashboard with default range', () => {
    const req = httpMock.expectOne(isDashboardRequest);
    expect(req.request.params.get('range')).toBe('30d');
    const mockData = buildDashboardData();
    req.flush(mockData);

    const state = service.state();
    expect(state.loading).toBeFalse();
    expect(state.error).toBeNull();
    expect(state.data).toBeTruthy();
    expect(state.data!.kpis.length).toBe(1);
    expect(state.data!.kpis[0].label).toBe('Total Balance');
  });

  it('should set lastUpdated timestamp on successful fetch', () => {
    const before = new Date().getTime();
    flushInitialRequest();

    const state = service.state();
    const lastUpdated = new Date(state.data!.lastUpdated).getTime();
    expect(lastUpdated).toBeGreaterThanOrEqual(before - 1000);
  });

  it('should handle HTTP errors gracefully and return generated fake data', fakeAsync(() => {
    const req = httpMock.expectOne(isDashboardRequest);
    req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    tick();

    const state = service.state();
    expect(state.data).toBeTruthy();
    expect(state.loading).toBeFalse();
    expect(state.data!.kpis.length).toBe(12);
    expect(state.data!.kpis[0].numericValue).toBeGreaterThan(0);
    expect(state.data!.recentActivity.length).toBeGreaterThan(0);
    expect(state.data!.fraudAlerts.length).toBeGreaterThan(0);
    expect(['operational', 'degraded']).toContain(state.data!.systemHealth.overall);
    flush();
  }));

  it('should handle network error and return generated fake data', fakeAsync(() => {
    const req = httpMock.expectOne(isDashboardRequest);
    req.error(new ProgressEvent('network error'));
    tick();

    const state = service.state();
    expect(state.data).toBeTruthy();
    expect(state.data!.kpis[0].numericValue).toBeGreaterThan(0);
    flush();
  }));

  it('should support setting time range to 7d', fakeAsync(() => {
    flushInitialRequest();

    service.setTimeRange('7d');
    TestBed.flushEffects();
    tick();

    const req = httpMock.expectOne(isDashboardRequest);
    expect(req.request.params.get('range')).toBe('7d');
    req.flush(buildDashboardData());
    tick();

    expect(service.state().loading).toBeFalse();
    expect(service.state().timeRange).toBe('7d');
    flush();
  }));

  it('should support setting time range to 90d', fakeAsync(() => {
    flushInitialRequest();

    service.setTimeRange('90d');
    TestBed.flushEffects();
    tick();

    const req = httpMock.expectOne(isDashboardRequest);
    expect(req.request.params.get('range')).toBe('90d');
    req.flush(buildDashboardData());
    tick();

    expect(service.state().timeRange).toBe('90d');
    flush();
  }));

  it('should support manual refresh', fakeAsync(() => {
    flushInitialRequest();

    service.refresh();
    tick();

    const req = httpMock.expectOne(isDashboardRequest);
    expect(req.request.params.get('range')).toBe('30d');
    req.flush(buildDashboardData());
    tick();

    expect(service.state().loading).toBeFalse();
    expect(service.state().data).toBeTruthy();
    flush();
  }));

  it('should auto-refresh on 30s interval', fakeAsync(() => {
    flushInitialRequest();

    tick(30_000);
    TestBed.flushEffects();

    const req = httpMock.expectOne(isDashboardRequest);
    req.flush(buildDashboardData());
    tick();

    expect(service.state().data).toBeTruthy();
    flush();
  }));

  it('should maintain correct state after sequential operations', fakeAsync(() => {
    // Initial load with default range
    const req1 = httpMock.expectOne(isDashboardRequest);
    expect(req1.request.params.get('range')).toBe('30d');
    req1.flush(buildDashboardData());

    expect(service.state().loading).toBeFalse();
    expect(service.state().data).toBeTruthy();
    expect(service.state().timeRange).toBe('30d');

    // Change range
    service.setTimeRange('90d');
    TestBed.flushEffects();
    tick();

    expect(service.state().loading).toBeTrue();

    const req2 = httpMock.expectOne(isDashboardRequest);
    expect(req2.request.params.get('range')).toBe('90d');
    req2.flush(buildDashboardData({ lastUpdated: '2026-07-12T12:00:00Z' }));
    tick();

    expect(service.state().loading).toBeFalse();
    expect(service.state().timeRange).toBe('90d');
    flush();
  }));

  it('should use current timeRange when refreshing after range change', fakeAsync(() => {
    flushInitialRequest();

    service.setTimeRange('7d');
    TestBed.flushEffects();
    tick();
    httpMock.expectOne(isDashboardRequest).flush(buildDashboardData());
    tick();

    // Manual refresh should still use 7d
    service.refresh();
    TestBed.flushEffects();
    tick();
    const req = httpMock.expectOne(isDashboardRequest);
    expect(req.request.params.get('range')).toBe('7d');
    req.flush(buildDashboardData());
    tick();
    flush();
  }));

  it('should set loading true when time range changes', fakeAsync(() => {
    flushInitialRequest();

    service.setTimeRange('7d');
    expect(service.state().loading).toBeTrue();

    TestBed.flushEffects();
    tick();
    httpMock.expectOne(isDashboardRequest).flush(buildDashboardData());
    tick();
    flush();
  }));

  it('should populate all dashboard sections from response', fakeAsync(() => {
    const fullData = buildDashboardData();
    const req = httpMock.expectOne(isDashboardRequest);
    req.flush(fullData);
    tick();

    const state = service.state();
    expect(state.data!.kpis).toEqual(fullData.kpis);
    expect(state.data!.paymentVolume.data.length).toBe(3);
    expect(state.data!.recentActivity.length).toBe(1);
    expect(state.data!.fraudAlerts.length).toBe(1);
    expect(state.data!.systemHealth.overall).toBe('operational');
    flush();
  }));
});
