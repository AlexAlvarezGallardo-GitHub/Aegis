import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

export type IconSize = 'sm' | 'md' | 'lg' | 'xl';

@Component({
  selector: 'app-aegis-icon',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-icon
      [class]="sizeClass()"
      [svgIcon]="isCustom() ? name() : undefined"
      [attr.aria-hidden]="decorative() ? 'true' : null"
      [attr.aria-label]="decorative() ? null : label()"
    >
      @if (!isCustom()) {
        {{ name() }}
      }
    </mat-icon>
  `,
  styleUrl: './aegis-icon.component.scss',
})
export class AegisIconComponent {
  readonly name = input.required<string>();
  readonly size = input<IconSize>('md');
  readonly decorative = input<boolean>(true);
  readonly label = input<string>('');
  readonly color = input<string>('inherit');

  readonly sizeClass = computed<string>(() => {
    return `icon-${this.size()}`;
  });

  readonly isCustom = computed<boolean>(() => {
    return this.name().startsWith('aegis-');
  });
}
