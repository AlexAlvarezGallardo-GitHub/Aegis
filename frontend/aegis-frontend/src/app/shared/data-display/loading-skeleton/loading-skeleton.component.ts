import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type SkeletonVariant = 'text' | 'circle' | 'rect' | 'card';

@Component({
  selector: 'app-loading-skeleton',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './loading-skeleton.component.html',
  styleUrl: './loading-skeleton.component.scss',
})
export class LoadingSkeletonComponent {
  readonly variant = input<SkeletonVariant>('text');
  readonly count = input<number>(1);
  readonly width = input<string>('100%');
  readonly height = input<string>('16px');
  readonly borderRadius = input<string>('var(--aegis-radius-md)');
}
