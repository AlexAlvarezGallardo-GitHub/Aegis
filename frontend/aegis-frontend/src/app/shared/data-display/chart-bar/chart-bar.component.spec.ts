import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChartBarComponent } from './chart-bar.component';
import { ChartDataPoint } from '../../models/dashboard.model';

describe('ChartBarComponent', () => {
  let component: ChartBarComponent;
  let fixture: ComponentFixture<ChartBarComponent>;

  const simpleData: ChartDataPoint[] = [
    { label: 'Mon', value: 50 },
    { label: 'Tue', value: 100 },
    { label: 'Wed', value: 75 },
  ];

  const stackedData: ChartDataPoint[] = [
    { label: 'Mon', value: 30, secondaryValue: 20, tertiaryValue: 10 },
    { label: 'Tue', value: 40, secondaryValue: 15, tertiaryValue: 5 },
  ];

  // ── Helpers ────────────────────────────────────────────────────────────

  function createComponent(
    data: ChartDataPoint[] = simpleData,
    inputs: Record<string, unknown> = {},
  ): void {
    fixture = TestBed.createComponent(ChartBarComponent);
    fixture.componentRef.setInput('data', data);
    for (const [key, val] of Object.entries(inputs)) {
      fixture.componentRef.setInput(key, val);
    }
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChartBarComponent],
    }).compileComponents();
  });

  // ── Creation ───────────────────────────────────────────────────────────

  it('should create', () => {
    createComponent();
    expect(component).toBeTruthy();
  });

  // ── Simple Bars ────────────────────────────────────────────────────────

  describe('Simple bars', () => {
    it('should render bars when data is provided', () => {
      createComponent();
      const barGroups = fixture.nativeElement.querySelectorAll('.bar-group');
      expect(barGroups.length).toBe(3);
    });

    it('should render single bars for each data point', () => {
      createComponent();
      const singleBars = fixture.nativeElement.querySelectorAll('.bar-single');
      expect(singleBars.length).toBe(3);
    });

    it('should render bar-primary class for simple bars', () => {
      createComponent();
      const primaryBars = fixture.nativeElement.querySelectorAll('.bar-primary');
      expect(primaryBars.length).toBe(3);
    });

    it('should not render stacked bars for simple data', () => {
      createComponent();
      const stacks = fixture.nativeElement.querySelectorAll('.bar-stack');
      expect(stacks.length).toBe(0);
    });

    it('should compute correct height percentages', () => {
      createComponent();
      // maxVal = 100 (max of 50, 100, 75)
      expect(component.percent(50)).toBe(50);
      expect(component.percent(100)).toBe(100);
      expect(component.percent(75)).toBe(75);
    });

    it('should render bar labels', () => {
      createComponent();
      const labels = fixture.nativeElement.querySelectorAll('.bar-label');
      expect(labels.length).toBe(3);
      expect(labels[0].textContent.trim()).toBe('Mon');
      expect(labels[1].textContent.trim()).toBe('Tue');
      expect(labels[2].textContent.trim()).toBe('Wed');
    });

    it('should apply custom bar color via inline style', () => {
      createComponent(simpleData, { barColor: 'red' });
      const primaryBars = fixture.nativeElement.querySelectorAll('.bar-primary');
      primaryBars.forEach((bar: Element) => {
        expect((bar as HTMLElement).style.background).toBe('red');
      });
    });

    it('should apply height style to container', () => {
      createComponent(simpleData, { height: '200px' });
      const container = fixture.nativeElement.querySelector('.chart-bar');
      expect(container.style.height).toBe('200px');
    });

    it('should set animation delay on bars based on index', () => {
      createComponent();
      const bars = fixture.nativeElement.querySelectorAll('.bar-primary');
      expect((bars[0] as HTMLElement).style.animationDelay).toBe('0ms');
      expect((bars[1] as HTMLElement).style.animationDelay).toBe('20ms');
      expect((bars[2] as HTMLElement).style.animationDelay).toBe('40ms');
    });
  });

  // ── Stacked Bars ───────────────────────────────────────────────────────

  describe('Stacked bars', () => {
    it('should render stacked bars when secondaryValue and tertiaryValue are present', () => {
      createComponent(stackedData);
      const stacks = fixture.nativeElement.querySelectorAll('.bar-stack');
      expect(stacks.length).toBe(2);
    });

    it('should render bar-success, bar-warning, bar-error for stacked segments', () => {
      createComponent(stackedData);
      const successBars = fixture.nativeElement.querySelectorAll('.bar-success');
      const warningBars = fixture.nativeElement.querySelectorAll('.bar-warning');
      const errorBars = fixture.nativeElement.querySelectorAll('.bar-error');
      expect(successBars.length).toBe(2);
      expect(warningBars.length).toBe(2);
      expect(errorBars.length).toBe(2);
    });

    it('should not render bar-single for stacked data', () => {
      createComponent(stackedData);
      const singles = fixture.nativeElement.querySelectorAll('.bar-single');
      expect(singles.length).toBe(0);
    });

    it('should compute correct stack percentages', () => {
      createComponent(stackedData);
      // maxVal = max(30+20+10, 40+15+5) = max(60, 60) = 60
      expect(component.stackPercent(30)).toBeCloseTo(50, 0);
      expect(component.stackPercent(60)).toBeCloseTo(100, 0);
    });

    it('should render labels for stacked bar groups', () => {
      createComponent(stackedData);
      const labels = fixture.nativeElement.querySelectorAll('.bar-label');
      expect(labels.length).toBe(2);
      expect(labels[0].textContent.trim()).toBe('Mon');
      expect(labels[1].textContent.trim()).toBe('Tue');
    });
  });

  // ── Empty Data ─────────────────────────────────────────────────────────

  describe('Empty data', () => {
    it('should render nothing when data is empty', () => {
      createComponent([]);
      const container = fixture.nativeElement.querySelector('.chart-bar');
      expect(container).toBeNull();
    });

    it('should not render any bar groups when data is empty', () => {
      createComponent([]);
      const groups = fixture.nativeElement.querySelectorAll('.bar-group');
      expect(groups.length).toBe(0);
    });

    it('should return maxVal of 1 when data is empty (division safety)', () => {
      createComponent([]);
      // percent() divides by maxVal, which should be 1 when data is empty
      expect(component.percent(50)).toBe(5000); // 50/1 * 100
    });
  });

  // ── Edge Cases ─────────────────────────────────────────────────────────

  describe('Edge cases', () => {
    it('should handle single data point', () => {
      createComponent([{ label: 'Only', value: 42 }]);
      const groups = fixture.nativeElement.querySelectorAll('.bar-group');
      expect(groups.length).toBe(1);
      expect(component.percent(42)).toBe(100);
    });

    it('should handle data with value 0', () => {
      createComponent([{ label: 'Zero', value: 0 }]);
      expect(component.percent(0)).toBe(0);
    });

    it('should handle data with partial stack values (only secondaryValue)', () => {
      // When only secondaryValue is set but not tertiaryValue, it should render simple bars
      const partialData: ChartDataPoint[] = [
        { label: 'A', value: 50, secondaryValue: 25 },
      ];
      createComponent(partialData);
      // Template checks both secondaryValue AND tertiaryValue !== undefined
      const singles = fixture.nativeElement.querySelectorAll('.bar-single');
      expect(singles.length).toBe(1);
    });
  });
});
