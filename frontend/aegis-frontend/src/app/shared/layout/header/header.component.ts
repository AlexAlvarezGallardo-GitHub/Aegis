import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { Router } from '@angular/router';
import { ThemeToggleComponent } from '../../components/theme-toggle/theme-toggle.component';
import { AuthService } from '../../../features/auth/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatBadgeModule,
    MatDividerModule,
    ThemeToggleComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  private router = inject(Router);
  private authService = inject(AuthService);

  readonly userEmail = signal<string>('');
  readonly notificationCount = signal<number>(0);
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

  onLogout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login']),
    });
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  private loadUser(): void {
    this.authService.getCurrentUser().subscribe({
      next: (user) => this.userEmail.set(user.email),
      error: () => { /* ignore */ },
    });
  }
}
