import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChartAreaComponent } from './chart-area.component';
import { ChartDataPoint } from '../../models/dashboard.model';

describe('ChartAreaComponent', () => {
  let component: ChartAreaComponent;
  let fixture: ComponentFixture<ChartAreaComponent>;

  const sampleData: ChartDataPoint[] = [
    { label: 'Mon', value: 20 },
    { label: 'Tue', value: 40 },
    { label: 'Wed', value: 60 },
    { label: 'Thu', value: 80 },
  ];

  // ── Helpers ────────────────────────────────────────────────────────────

  function createComponent(
    data: ChartDataPoint[] = sampleData,
    inputs: Record<string, unknown> = {},
  ): void {
    fixture = TestBed.createComponent(ChartAreaComponent);
    fixture.componentRef.setInput('data', data);
    for (const [key, val] of Object.entries(inputs)) {
      fixture.componentRef.setInput(key, val);
    }
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChartAreaComponent],
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

    it('should render the chart-area container div', () => {
      createComponent();
      const container = fixture.nativeElement.querySelector('.chart-area');
      expect(container).toBeTruthy();
    });

    it('should apply height style to container', () => {
      createComponent(sampleData, { height: '250px' });
      const container = fixture.nativeElement.querySelector('.chart-area');
      expect(container.style.height).toBe('250px');
    });

    it('should set the SVG viewBox attribute', () => {
      createComponent();
      const svg = fixture.nativeElement.querySelector('svg');
      // 4 data points → viewBoxWidth = 4*20+20 = 100
      expect(svg.getAttribute('viewBox')).toBe('0 0 100 100');
    });

    it('should render a line path element', () => {
      createComponent();
      const linePath = fixture.nativeElement.querySelector('.chart-line');
      expect(linePath).toBeTruthy();
    });

    it('should render a filled area path element', () => {
      createComponent();
      const areaPath = fixture.nativeElement.querySelector('.chart-fill');
      expect(areaPath).toBeTruthy();
    });

    it('should apply stroke color to the line path', () => {
      createComponent(sampleData, { stroke: '#aabbcc' });
      const linePath = fixture.nativeElement.querySelector('.chart-line');
      expect(linePath.getAttribute('stroke')).toBe('#aabbcc');
    });

    it('should set gradient stop colors from inputs', () => {
      createComponent(sampleData, {
        gradientTop: 'rgba(100,200,50,0.6)',
        gradientBottom: 'rgba(100,200,50,0)',
      });
      const stops = fixture.nativeElement.querySelectorAll('stop');
      expect(stops.length).toBe(2);
      expect(stops[0].getAttribute('stop-color')).toBe('rgba(100,200,50,0.6)');
      expect(stops[1].getAttribute('stop-color')).toBe('rgba(100,200,50,0)');
    });

    it('should render a linear gradient definition', () => {
      createComponent();
      const gradient = fixture.nativeElement.querySelector('linearGradient');
      expect(gradient).toBeTruthy();
    });

    it('should set preserveAspectRatio to none on SVG', () => {
      createComponent();
      const svg = fixture.nativeElement.querySelector('svg');
      expect(svg.getAttribute('preserveAspectRatio')).toBe('none');
    });
  });

  // ── Empty Data ─────────────────────────────────────────────────────────

  describe('Empty data', () => {
    it('should render nothing when data is empty', () => {
      createComponent([]);
      const svg = fixture.nativeElement.querySelector('svg');
      expect(svg).toBeNull();
    });

    it('should not render chart-area container when data is empty', () => {
      createComponent([]);
      const container = fixture.nativeElement.querySelector('.chart-area');
      expect(container).toBeNull();
    });

    it('should compute empty paths when data is empty', () => {
      createComponent([]);
      expect(component.linePath()).toBe('');
      expect(component.areaPath()).toBe('');
    });
  });

  // ── Threshold Line ─────────────────────────────────────────────────────

  describe('Threshold line', () => {
    it('should not render threshold line by default', () => {
      createComponent();
      const line = fixture.nativeElement.querySelector('line');
      expect(line).toBeNull();
    });

    it('should not render threshold text by default', () => {
      createComponent();
      const text = fixture.nativeElement.querySelector('text');
      expect(text).toBeNull();
    });

    it('should render threshold line when showThreshold is true', () => {
      createComponent(sampleData, { showThreshold: true, thresholdValue: 50 });
      const line = fixture.nativeElement.querySelector('line');
      expect(line).toBeTruthy();
    });

    it('should render threshold text label when showThreshold is true', () => {
      createComponent(sampleData, { showThreshold: true, thresholdValue: 50 });
      const text = fixture.nativeElement.querySelector('text');
      expect(text).toBeTruthy();
      expect(text.textContent.trim()).toBe('Threshold');
    });

    it('should apply dashed stroke to threshold line', () => {
      createComponent(sampleData, { showThreshold: true, thresholdValue: 50 });
      const line = fixture.nativeElement.querySelector('line');
      expect(line.getAttribute('stroke-dasharray')).toBe('4,4');
    });

    it('should style threshold line with error color', () => {
      createComponent(sampleData, { showThreshold: true, thresholdValue: 50 });
      const line = fixture.nativeElement.querySelector('line');
      expect(line.getAttribute('stroke')).toBe('var(--aegis-color-error)');
    });

    it('should set threshold line opacity to 0.5', () => {
      createComponent(sampleData, { showThreshold: true, thresholdValue: 50 });
      const line = fixture.nativeElement.querySelector('line');
      expect(line.getAttribute('opacity')).toBe('0.5');
    });

    it('should compute thresholdY based on thresholdValue and data max', () => {
      // Data: 20, 40, 60, 80 → max = 80
      // thresholdValue = 40, padding = 4, usableH = 92
      // thresholdY = 100 - 4 - (40/80)*92 = 96 - 46 = 50
      createComponent(sampleData, { showThreshold: true, thresholdValue: 40 });
      expect(component.thresholdY()).toBeCloseTo(50, 0);
    });

    it('should position threshold line using computed thresholdY', () => {
      createComponent(sampleData, { showThreshold: true, thresholdValue: 40 });
      const line = fixture.nativeElement.querySelector('line');
      const y = parseFloat(line.getAttribute('y1'));
      expect(y).toBeCloseTo(component.thresholdY(), 0);
    });

    it('should position threshold text above the line', () => {
      createComponent(sampleData, { showThreshold: true, thresholdValue: 40 });
      const text = fixture.nativeElement.querySelector('text');
      const textY = parseFloat(text.getAttribute('y'));
      // Text Y should be thresholdY - 4
      expect(textY).toBeCloseTo(component.thresholdY() - 4, 0);
    });

    it('should style threshold text with error color and small font', () => {
      createComponent(sampleData, { showThreshold: true, thresholdValue: 50 });
      const text = fixture.nativeElement.querySelector('text');
      expect(text.getAttribute('fill')).toBe('var(--aegis-color-error)');
      expect(text.getAttribute('font-size')).toBe('8');
      expect(text.getAttribute('opacity')).toBe('0.6');
    });
  });

  // ── SVG Path Computation ───────────────────────────────────────────────

  describe('SVG path computation', () => {
    it('should compute correct viewBoxWidth based on data length', () => {
      createComponent();
      // 4 data points → viewBoxWidth = 4*20+20 = 100
      expect(component.viewBoxWidth()).toBe(100);
    });

    it('should compute correct viewBox string', () => {
      createComponent();
      expect(component.viewBox()).toBe('0 0 100 100');
    });

    it('should compute non-empty linePath for valid data', () => {
      createComponent();
      const path = component.linePath();
      expect(path).toBeTruthy();
      expect(path.startsWith('M')).toBeTrue();
      expect(path).toContain('L');
    });

    it('should compute non-empty areaPath that closes with Z', () => {
      createComponent();
      const area = component.areaPath();
      expect(area).toBeTruthy();
      expect(area.endsWith('Z')).toBeTrue();
    });

    it('should produce a linePath with correct number of segments', () => {
      createComponent();
      const path = component.linePath();
      // 4 data points → 1 M command + 3 L commands
      const mCount = (path.match(/M/g) || []).length;
      const lCount = (path.match(/L/g) || []).length;
      expect(mCount).toBe(1);
      expect(lCount).toBe(3);
    });

    it('should scale Y values relative to the maximum', () => {
      createComponent();
      const path = component.linePath();
      // Values: 20, 40, 60, 80 → max = 80
      // Y should decrease as value increases (SVG Y axis is inverted)
      // Extract Y coordinates from path
      const coords = path.match(/[\d.]+,[\d.]+/g)!;
      const yValues = coords.map((c) => parseFloat(c.split(',')[1]));
      // Y values should be decreasing (higher value → lower Y)
      for (let i = 1; i < yValues.length; i++) {
        expect(yValues[i]).toBeLessThan(yValues[i - 1]);
      }
    });

    it('should handle single data point', () => {
      createComponent([{ label: 'Only', value: 50 }]);
      expect(component.linePath()).toBeTruthy();
      expect(component.viewBoxWidth()).toBe(40); // 1*20+20
    });

    it('should handle data with all equal values', () => {
      const equalData: ChartDataPoint[] = [
        { label: 'A', value: 25 },
        { label: 'B', value: 25 },
        { label: 'C', value: 25 },
      ];
      createComponent(equalData);
      const path = component.linePath();
      // Extract Y coordinates — should all be the same
      const coords = path.match(/[\d.]+,[\d.]+/g)!;
      const yValues = coords.map((c) => parseFloat(c.split(',')[1]));
      for (let i = 1; i < yValues.length; i++) {
        expect(yValues[i]).toBeCloseTo(yValues[0], 1);
      }
    });

    it('should respect custom padding input', () => {
      const localFixture = TestBed.createComponent(ChartAreaComponent);
      localFixture.componentRef.setInput('data', sampleData);
      localFixture.componentRef.setInput('padding', 10);
      const localComponent = localFixture.componentInstance;
      // Read computed BEFORE first detectChanges to avoid caching with defaults
      const path = localComponent.linePath();
      expect(path).toContain('M10.0,');
      localFixture.detectChanges();
    });
  });

  // ── Threshold Y Edge Cases ─────────────────────────────────────────────

  describe('Threshold Y edge cases', () => {
    it('should return 50 as default thresholdY when data is empty', () => {
      createComponent([]);
      expect(component.thresholdY()).toBe(50);
    });

    it('should place threshold at bottom when thresholdValue equals max', () => {
      // Data: 20, 40, 60, 80 → max = 80
      // thresholdValue = 80 → thresholdY = 100 - 4 - (80/80)*92 = 96 - 92 = 4
      createComponent(sampleData, { showThreshold: true, thresholdValue: 80 });
      expect(component.thresholdY()).toBeCloseTo(4, 0);
    });

    it('should place threshold near top when thresholdValue is small relative to max', () => {
      // Data: 20, 40, 60, 80 → max = 80
      // thresholdValue = 1 → thresholdY = 100 - 4 - (1/80)*92 ≈ 96 - 1.15 ≈ 94.85
      createComponent(sampleData, { showThreshold: true, thresholdValue: 1 });
      expect(component.thresholdY()).toBeGreaterThan(90);
    });
  });
});
