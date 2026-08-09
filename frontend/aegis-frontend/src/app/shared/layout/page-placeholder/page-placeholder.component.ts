import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { EmptyStateComponent } from '../../data-display/empty-state/empty-state.component';

@Component({
  selector: 'app-page-placeholder',
  standalone: true,
  imports: [CommonModule, EmptyStateComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-placeholder">
      <app-empty-state
        icon="construction"
        [title]="title()"
        description="This page is under construction."
      />
    </div>
  `,
  styles: [`
    .page-placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 60vh;
      padding: var(--aegis-space-8);
    }
  `],
})
export class PagePlaceholderComponent {
  private route = inject(ActivatedRoute);

  readonly title = signal(this.route.snapshot.data['title'] ?? 'Page');
}
