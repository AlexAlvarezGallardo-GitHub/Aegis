import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

export type ChipVariant = 'neutral' | 'success' | 'warning' | 'error' | 'info' | 'gold';
export type ChipSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-status-chip',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './status-chip.component.html',
  styleUrl: './status-chip.component.scss',
})
export class StatusChipComponent {
  readonly label = input.required<string>();
  readonly variant = input<ChipVariant>('neutral');
  readonly size = input<ChipSize>('md');
  readonly icon = input<string>();
  readonly pulse = input<boolean>(false);
}
