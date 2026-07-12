import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChartLineComponent } from './chart-line.component';
import { ChartDataPoint } from '../../models/dashboard.model';

describe('ChartLineComponent', () => {
  let component: ChartLineComponent;
  let fixture: ComponentFixture<ChartLineComponent>;

  const sampleData: ChartDataPoint[] = [
    { label: 'Mon', value: 10 },
    { label: 'Tue', value: 20 },
    { label: 'Wed', value: 30 },
  ];

  // ── Helpers ────────────────────────────────────────────────────────────

  function createComponent(data: ChartDataPoint[] = sampleData): void {
    fixture = TestBed.createComponent(ChartLineComponent);
    fixture.componentRef.setInput('data', data);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChartLineComponent],
    }).compileComponents();
  });

  // ── Creation ───────────────────────────────────────────────────────────

  it('should create', () => {
    createComponent();
    expect(component).toBeTruthy();
  });

  // ── SVG Rendering ──────────────────────────────────────────────────────

  describe('SVG rendering', () => {
    it('should render SVG when data is provided', () => {
      createComponent();
      const svg = fixture.nativeElement.querySelector('svg');
      expect(svg).toBeTruthy();
    });

    it('should render the chart-line container div', () => {
      createComponent();
      const container = fixture.nativeElement.querySelector('.chart-line');
      expect(container).toBeTruthy();
    });

    it('should apply height style to container', () => {
      fixture = TestBed.createComponent(ChartLineComponent);
      fixture.componentRef.setInput('data', sampleData);
      fixture.componentRef.setInput('height', '200px');
      fixture.detectChanges();
      const container = fixture.nativeElement.querySelector('.chart-line');
      expect(container.style.height).toBe('200px');
    });

    it('should render a line path element', () => {
      createComponent();
      const paths = fixture.nativeElement.querySelectorAll('path');
      // 2 paths: area fill + line stroke
      expect(paths.length).toBe(2);
    });

    it('should apply stroke color to the line path', () => {
      fixture = TestBed.createComponent(ChartLineComponent);
      fixture.componentRef.setInput('data', sampleData);
      fixture.componentRef.setInput('stroke', '#ff0000');
      fixture.detectChanges();
      const strokePath = fixture.nativeElement.querySelector('.chart-stroke');
      expect(strokePath.getAttribute('stroke')).toBe('#ff0000');
    });

    it('should render circle dots for each data point', () => {
      createComponent();
      const circles = fixture.nativeElement.querySelectorAll('circle.chart-dot');
      expect(circles.length).toBe(3);
    });

    it('should apply stroke color to dots', () => {
      fixture = TestBed.createComponent(ChartLineComponent);
      fixture.componentRef.setInput('data', sampleData);
      fixture.componentRef.setInput('stroke', '#00ff00');
      fixture.detectChanges();
      const circles = fixture.nativeElement.querySelectorAll('circle.chart-dot');
      circles.forEach((c: Element) => {
        expect(c.getAttribute('fill')).toBe('#00ff00');
      });
    });

    it('should render a linear gradient definition', () => {
      createComponent();
      const gradient = fixture.nativeElement.querySelector('linearGradient');
      expect(gradient).toBeTruthy();
    });

    it('should set gradient stop colors from inputs', () => {
      fixture = TestBed.createComponent(ChartLineComponent);
      fixture.componentRef.setInput('data', sampleData);
      fixture.componentRef.setInput('gradientTop', 'rgba(255,0,0,0.5)');
      fixture.componentRef.setInput('gradientBottom', 'rgba(0,0,255,0)');
      fixture.detectChanges();
      const stops = fixture.nativeElement.querySelectorAll('stop');
      expect(stops.length).toBe(2);
      expect(stops[0].getAttribute('stop-color')).toBe('rgba(255,0,0,0.5)');
      expect(stops[1].getAttribute('stop-color')).toBe('rgba(0,0,255,0)');
    });
  });

  // ── Empty Data ─────────────────────────────────────────────────────────

  describe('Empty data', () => {
    it('should render nothing when data is empty', () => {
      createComponent([]);
      const svg = fixture.nativeElement.querySelector('svg');
      expect(svg).toBeNull();
    });

    it('should not render chart-line container when data is empty', () => {
      createComponent([]);
      const container = fixture.nativeElement.querySelector('.chart-line');
      expect(container).toBeNull();
    });
  });

  // ── SVG Path Computation ───────────────────────────────────────────────

  describe('SVG path computation', () => {
    it('should compute correct viewBox based on data length', () => {
      createComponent();
      // 3 data points → viewBox = '0 0 80 100' (3*20+20 = 80)
      expect(component.viewBox()).toBe('0 0 80 100');
    });

    it('should compute non-empty linePath for valid data', () => {
      createComponent();
      const path = component.linePath();
      expect(path).toBeTruthy();
      expect(path.startsWith('M')).toBeTrue();
      // Should contain L commands for subsequent points
      expect(path).toContain('L');
    });

    it('should compute non-empty areaPath that closes with Z', () => {
      createComponent();
      const area = component.areaPath();
      expect(area).toBeTruthy();
      expect(area.endsWith('Z')).toBeTrue();
    });

    it('should compute correct number of dots', () => {
      createComponent();
      expect(component.dots().length).toBe(3);
    });

    it('should compute line path with correct starting point', () => {
      createComponent();
      const path = component.linePath();
      // First point: x = padding(4) + 0 = 4.0, y = 100 - 4 - (10/30)*92 = 96 - 30.67 = 65.3
      expect(path).toContain('M4.0,');
    });

    it('should compute dots with correct coordinates', () => {
      createComponent();
      const dots = component.dots();
      // First dot: x = 4.0
      expect(dots[0].x).toBeCloseTo(4.0, 0);
      // Last dot x should be near the right edge
      expect(dots[2].x).toBeGreaterThan(50);
    });

    it('should scale Y values relative to the maximum', () => {
      createComponent();
      const dots = component.dots();
      // Values: 10, 20, 30 → max = 30
      // Y should decrease as value increases (SVG Y axis is inverted)
      expect(dots[0].y).toBeGreaterThan(dots[1].y);
      expect(dots[1].y).toBeGreaterThan(dots[2].y);
    });

    it('should handle single data point', () => {
      createComponent([{ label: 'Only', value: 50 }]);
      expect(component.linePath()).toBeTruthy();
      expect(component.dots().length).toBe(1);
    });

    it('should handle data with all equal values', () => {
      const equalData: ChartDataPoint[] = [
        { label: 'A', value: 10 },
        { label: 'B', value: 10 },
        { label: 'C', value: 10 },
      ];
      createComponent(equalData);
      const dots = component.dots();
      // All Y values should be the same
      expect(dots[0].y).toBeCloseTo(dots[1].y, 1);
      expect(dots[1].y).toBeCloseTo(dots[2].y, 1);
    });

    it('should respect custom padding input', () => {
      const localFixture = TestBed.createComponent(ChartLineComponent);
      localFixture.componentRef.setInput('data', sampleData);
      localFixture.componentRef.setInput('padding', 10);
      const localComponent = localFixture.componentInstance;
      // Read computed BEFORE first detectChanges to avoid caching with defaults
      const dots = localComponent.dots();
      expect(dots[0].x).toBeCloseTo(10, 0);
      localFixture.detectChanges();
    });
  });
});
