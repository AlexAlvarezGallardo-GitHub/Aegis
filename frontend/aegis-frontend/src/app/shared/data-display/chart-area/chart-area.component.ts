import { Component, ChangeDetectionStrategy, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartDataPoint } from '../../models/dashboard.model';

@Component({
  selector: 'app-chart-area',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (data().length > 0) {
      <div class="chart-area" [style.height]="height()">
        <svg
          [attr.viewBox]="viewBox()"
          preserveAspectRatio="none"
          class="chart-svg"
        >
          <defs>
            <linearGradient [attr.id]="gradientId" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" [attr.stop-color]="gradientTop()" stop-opacity="0.4"/>
              <stop offset="100%" [attr.stop-color]="gradientBottom()" stop-opacity="0"/>
            </linearGradient>
          </defs>
          @if (showThreshold()) {
            <line
              [attr.x1]="padding()"
              [attr.y1]="thresholdY()"
              [attr.x2]="viewBoxWidth() - padding()"
              [attr.y2]="thresholdY()"
              stroke="var(--aegis-color-error)"
              stroke-width="1"
              stroke-dasharray="4,4"
              opacity="0.5"
            />
            <text
              [attr.x]="padding() + 2"
              [attr.y]="thresholdY() - 4"
              fill="var(--aegis-color-error)"
              font-size="8"
              opacity="0.6"
            >Threshold</text>
          }
          <path
            [attr.d]="areaPath()"
            [attr.fill]="'url(#' + gradientId + ')'"
            class="chart-fill"
          />
          <path
            [attr.d]="linePath()"
            [attr.stroke]="stroke()"
            stroke-width="2"
            fill="none"
            class="chart-line"
          />
        </svg>
      </div>
    }
  `,
  styles: [`
    .chart-area { width: 100%; position: relative; }
    .chart-svg { width: 100%; height: 100%; }
    .chart-fill { opacity: 0.8; }
    .chart-line { stroke-linecap: round; stroke-linejoin: round; }
  `],
})
export class ChartAreaComponent {
  readonly data = input.required<ChartDataPoint[]>();
  readonly stroke = input('var(--aegis-color-error)');
  readonly gradientTop = input('rgba(239, 68, 68, 0.3)');
  readonly gradientBottom = input('rgba(239, 68, 68, 0)');
  readonly height = input('140px');
  readonly showThreshold = input(false);
  readonly thresholdValue = input(70);
  readonly padding = input(4);

  readonly gradientId = 'area-grad-' + Math.random().toString(36).slice(2, 8);

  readonly viewBoxWidth = computed(() => this.data().length * 20 + 20);

  readonly viewBox = computed(() => `0 0 ${this.viewBoxWidth()} 100`);

  private readonly points = computed(() => {
    const d = this.data();
    if (d.length === 0) return { line: '', area: '' };
    const max = Math.max(...d.map((p) => p.value), 1);
    const p = this.padding();
    const w = this.viewBoxWidth();
    const h = 100;
    const usableH = h - p * 2;
    const usableW = w - p * 2;

    const pts = d.map((pt, i) => {
      const x = p + (i / (d.length - 1 || 1)) * usableW;
      const y = h - p - (pt.value / max) * usableH;
      return { x, y };
    });

    const lineD = pts.map((pt, i) => `${i === 0 ? 'M' : 'L'}${pt.x.toFixed(1)},${pt.y.toFixed(1)}`).join(' ');
    const areaD = `${lineD} L${pts[pts.length - 1].x},${h} L${pts[0].x},${h} Z`;

    return { line: lineD, area: areaD };
  });

  readonly linePath = computed(() => this.points().line);
  readonly areaPath = computed(() => this.points().area);
  readonly thresholdY = computed(() => {
    const d = this.data();
    if (d.length === 0) return 50;
    const max = Math.max(...d.map((p) => p.value), 1);
    const h = 100;
    const p = this.padding();
    const usableH = h - p * 2;
    return h - p - (this.thresholdValue() / max) * usableH;
  });
}
