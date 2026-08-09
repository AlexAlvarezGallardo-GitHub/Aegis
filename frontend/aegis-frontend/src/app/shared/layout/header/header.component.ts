import { Component, ChangeDetectionStrategy, inject, signal, output, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
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
  readonly menuOpen = signal<boolean>(false);
  readonly currentPage = signal<string>(this.readRouteTitle());

  constructor() {
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.currentPage.set(this.readRouteTitle()));

    this.loadUser();
  }

  get userInitials(): string {
    const email = this.userEmail();
    if (!email) return 'AU';
    return email.substring(0, 2).toUpperCase();
  }

  readonly environment = environment.production ? 'PROD' : 'DEV';

  get environmentClass(): string {
    return this.environment === 'PROD' ? 'env-prod' : 'env-dev';
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

  private readRouteTitle(): string {
    let route = this.router.routerState.snapshot.root;
    while (route.firstChild) {
      route = route.firstChild;
    }
    const title = route.data['title'] as string | undefined;
    if (title) return title;

    const segments = this.router.url.split('/').filter(Boolean);
    if (segments.length === 0) return 'Dashboard';
    return segments[0].charAt(0).toUpperCase() + segments[0].slice(1);
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
