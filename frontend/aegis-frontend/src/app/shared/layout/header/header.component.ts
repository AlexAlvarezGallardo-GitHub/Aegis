import { Component, ChangeDetectionStrategy, inject, signal, output, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { Router } from '@angular/router';
import { AuthService } from '../../../features/auth/auth.service';
import { CommandPaletteService } from '../../services/command-palette.service';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatBadgeModule,
    MatDividerModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  private router = inject(Router);
  private authService = inject(AuthService);
  private commandPaletteService = inject(CommandPaletteService);
  private destroyRef = inject(DestroyRef);

  readonly menuToggle = output<void>();

  readonly userEmail = signal<string>('');
  readonly notificationCount = signal<number>(3);
  readonly menuOpen = signal<boolean>(false);

  constructor() {
    this.loadUser();
  }

  get currentPage(): string {
    const url = this.router.url;
    const segments = url.split('/').filter(Boolean);
    if (segments.length === 0) return 'Dashboard';
    return segments[0].charAt(0).toUpperCase() + segments[0].slice(1);
  }

  get userInitials(): string {
    const email = this.userEmail();
    if (!email) return 'AU';
    return email.substring(0, 2).toUpperCase();
  }

  readonly environment = environment.production ? 'PROD' : 'DEV';

  get environmentClass(): string {
    const env = this.environment;
    if (env === 'PROD') return 'env-prod';
    if (env === 'STAGING') return 'env-staging';
    return 'env-dev';
  }

  openSearch(): void {
    this.commandPaletteService.toggle();
  }

  onLogout(): void {
    this.authService.logout()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.router.navigate(['/login']),
        error: () => this.router.navigate(['/login']),
      });
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  private loadUser(): void {
    this.authService.getCurrentUser()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (user) => this.userEmail.set(user.email),
        error: () => { /* ignore */ },
      });
  }
}
