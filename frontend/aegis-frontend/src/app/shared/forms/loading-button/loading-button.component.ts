import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

export type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';

@Component({
  selector: 'app-aegis-loading-button',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatProgressSpinnerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './loading-button.component.html',
  styleUrl: './loading-button.component.scss',
})
export class LoadingButtonComponent {
  readonly loading = input<boolean>(false);
  readonly disabled = input<boolean>(false);
  readonly label = input<string>('');
  readonly variant = input<ButtonVariant>('primary');
  readonly fullWidth = input<boolean>(false);
  readonly type = input<'button' | 'submit'>('submit');
  readonly spinnerDiameter = input<number>(20);

  readonly variantColor = computed<string | undefined>(() => {
    const v = this.variant();
    if (v === 'primary') return 'primary';
    if (v === 'secondary') return 'accent';
    if (v === 'danger') return 'warn';
    return undefined;
  });
}
