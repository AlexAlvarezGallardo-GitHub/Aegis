import { Component, ChangeDetectionStrategy, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartDataPoint } from '../../models/dashboard.model';

@Component({
  selector: 'app-chart-bar',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (data().length > 0) {
      <div class="chart-bar" [style.height]="height()">
        @for (point of data(); track $index) {
          <div class="bar-group">
            @if (point.secondaryValue !== undefined && point.tertiaryValue !== undefined) {
              <div class="bar-stack">
                <div
                  class="bar bar-success"
                  [style.height.%]="stackPercent(point.value)"
                  [style.animation-delay]="$index * 20 + 'ms'"
                ></div>
                <div
                  class="bar bar-warning"
                  [style.height.%]="stackPercent(point.secondaryValue)"
                  [style.animation-delay]="$index * 20 + 'ms'"
                ></div>
                <div
                  class="bar bar-error"
                  [style.height.%]="stackPercent(point.tertiaryValue)"
                  [style.animation-delay]="$index * 20 + 'ms'"
                ></div>
              </div>
            } @else {
              <div class="bar-single">
                <div
                  class="bar bar-primary"
                  [style.height.%]="percent(point.value)"
                  [style.animation-delay]="$index * 20 + 'ms'"
                  [style.background]="barColor()"
                ></div>
              </div>
            }
            <span class="bar-label">{{ point.label }}</span>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .chart-bar {
      display: flex;
      align-items: flex-end;
      gap: 4px;
      width: 100%;
    }
    .bar-group {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      height: 100%;
      justify-content: flex-end;
    }
    .bar-stack {
      width: 100%;
      max-width: 32px;
      display: flex;
      flex-direction: column-reverse;
      align-items: center;
      border-radius: 3px 3px 0 0;
      overflow: hidden;
    }
    .bar-single {
      width: 100%;
      max-width: 32px;
      display: flex;
      align-items: flex-end;
      justify-content: center;
    }
    .bar {
      width: 100%;
      min-height: 2px;
      border-radius: 2px 2px 0 0;
      animation: bar-grow 500ms var(--aegis-ease-out) both;
    }
    .bar-primary { background: linear-gradient(to top, rgba(59, 130, 246, 0.4), rgba(59, 130, 246, 0.8)); }
    .bar-success { background: linear-gradient(to top, rgba(34, 197, 94, 0.4), rgba(34, 197, 94, 0.8)); }
    .bar-warning { background: linear-gradient(to top, rgba(245, 158, 11, 0.4), rgba(245, 158, 11, 0.8)); }
    .bar-error { background: linear-gradient(to top, rgba(239, 68, 68, 0.4), rgba(239, 68, 68, 0.8)); }
    .bar-label {
      font-size: 9px;
      color: var(--aegis-color-text-muted);
      text-transform: uppercase;
      white-space: nowrap;
    }
    @media (prefers-reduced-motion: reduce) {
      .bar { animation: none; }
    }
    @keyframes bar-grow {
      from { height: 0; opacity: 0; }
      to { opacity: 1; }
    }
  `],
})
export class ChartBarComponent {
  readonly data = input.required<ChartDataPoint[]>();
  readonly barColor = input('linear-gradient(to top, var(--aegis-color-info), rgba(59, 130, 246, 0.6))');
  readonly height = input('140px');

  private readonly maxVal = computed(() => {
    const d = this.data();
    if (d.length === 0) return 1;
    let max = 0;
    for (const pt of d) {
      const total = pt.value + (pt.secondaryValue ?? 0) + (pt.tertiaryValue ?? 0);
      if (total > max) max = total;
    }
    return max || 1;
  });

  percent(val: number): number {
    return (val / this.maxVal()) * 100;
  }

  stackPercent(val: number): number {
    return (val / this.maxVal()) * 100;
  }
}
