import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-page-placeholder',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-placeholder">
      <mat-icon class="placeholder-icon">construction</mat-icon>
      <h1 class="placeholder-title">{{ title() }}</h1>
      <p class="placeholder-description">This page is under construction.</p>
    </div>
  `,
  styles: [`
    .page-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 60vh;
      color: var(--aegis-color-text-muted, #94a3b8);
    }
    .placeholder-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      margin-bottom: 16px;
      opacity: 0.5;
    }
    .placeholder-title {
      font-size: 1.5rem;
      font-weight: 600;
      margin: 0 0 8px;
      color: var(--aegis-color-text, #e2e8f0);
    }
    .placeholder-description {
      font-size: 0.875rem;
      margin: 0;
    }
  `],
})
export class PagePlaceholderComponent {
  private route = inject(ActivatedRoute);

  readonly title = signal(this.route.snapshot.data['title'] ?? 'Page');
}
