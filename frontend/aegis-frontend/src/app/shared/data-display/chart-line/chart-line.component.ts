import { Component, ChangeDetectionStrategy, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartDataPoint } from '../../models/dashboard.model';

@Component({
  selector: 'app-chart-line',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (data().length > 0) {
      <div class="chart-line" [style.height]="height()">
        <svg
          [attr.viewBox]="viewBox()"
          preserveAspectRatio="none"
          class="chart-svg"
        >
          <defs>
            <linearGradient [attr.id]="gradientId()" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" [attr.stop-color]="gradientTop()" stop-opacity="1"/>
              <stop offset="100%" [attr.stop-color]="gradientBottom()" stop-opacity="0"/>
            </linearGradient>
          </defs>
          <path
            [attr.d]="areaPath()"
            [attr.fill]="'url(#' + gradientId() + ')'"
            class="chart-area"
          />
          <path
            [attr.d]="linePath()"
            [attr.stroke]="stroke()"
            stroke-width="2"
            fill="none"
            class="chart-stroke"
          />
          @for (dot of dots(); track $index) {
            <circle
              [attr.cx]="dot.x"
              [attr.cy]="dot.y"
              r="3"
              [attr.fill]="stroke()"
              class="chart-dot"
            />
          }
        </svg>
      </div>
    }
  `,
  styles: [`
    .chart-line { width: 100%; position: relative; }
    .chart-svg { width: 100%; height: 100%; }
    .chart-area { opacity: 0.8; }
    .chart-stroke { stroke-linecap: round; stroke-linejoin: round; }
    .chart-dot { opacity: 0; transition: opacity 150ms ease; }
    .chart-line:hover .chart-dot { opacity: 1; }
  `],
})
export class ChartLineComponent {
  readonly data = input.required<ChartDataPoint[]>();
  readonly stroke = input('var(--aegis-gold-500)');
  readonly gradientTop = input('rgba(212, 168, 67, 0.3)');
  readonly gradientBottom = input('rgba(212, 168, 67, 0)');
  readonly height = input('140px');
  readonly padding = input(4);

  readonly gradientId = computed(() => `line-grad-${Math.random().toString(36).slice(2, 8)}`);

  readonly viewBox = computed(() => {
    const len = this.data().length;
    return `0 0 ${len * 20 + 20} 100`;
  });

  private readonly points = computed(() => {
    const d = this.data();
    if (d.length === 0) return { line: '', area: '', dots: [] as { x: number; y: number }[] };
    const max = Math.max(...d.map((p) => p.value), 1);
    const p = this.padding();
    const w = d.length * 20;
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

    return { line: lineD, area: areaD, dots: pts };
  });

  readonly linePath = computed(() => this.points().line);
  readonly areaPath = computed(() => this.points().area);
  readonly dots = computed(() => this.points().dots);
}
