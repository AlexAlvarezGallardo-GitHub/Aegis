import { Component, ChangeDetectionStrategy, input, ContentChild, TemplateRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './empty-state.component.html',
  styleUrl: './empty-state.component.scss',
})
export class EmptyStateComponent {
  private router = inject(Router);

  readonly icon = input<string>('inbox');
  readonly title = input.required<string>();
  readonly description = input<string>('');
  readonly actionLabel = input<string>('');
  readonly actionRoute = input<string>();

  @ContentChild('illustration') illustrationTemplate?: TemplateRef<unknown>;

  onAction(): void {
    const route = this.actionRoute();
    if (route) {
      this.router.navigate([route]);
    }
  }
}
