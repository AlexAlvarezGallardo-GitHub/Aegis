import { Component, ChangeDetectionStrategy, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

export type TrendDirection = 'up' | 'down' | 'flat';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './stat-card.component.html',
  styleUrl: './stat-card.component.scss',
})
export class StatCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<string | number>();
  readonly icon = input<string>('trending_up');
  readonly trend = input<TrendDirection>();
  readonly trendValue = input<string>();

  readonly trendIcon = computed<string>(() => {
    const t = this.trend();
    if (t === 'up') return 'trending_up';
    if (t === 'down') return 'trending_down';
    return 'trending_flat';
  });
}
