import { Component, ChangeDetectionStrategy, input, ContentChild, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';
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
  readonly icon = input<string>('inbox');
  readonly title = input.required<string>();
  readonly description = input<string>('');
  readonly actionLabel = input<string>('');
  readonly actionRoute = input<string>();

  @ContentChild('illustration') illustrationTemplate?: TemplateRef<unknown>;

  onAction(): void {
    const route = this.actionRoute();
    if (route) {
      // Navigation handled by parent via routerLink or custom handler
      window.location.hash = route;
    }
  }
}
