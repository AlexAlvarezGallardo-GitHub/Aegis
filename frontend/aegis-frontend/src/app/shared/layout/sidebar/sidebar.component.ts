import { Component, ChangeDetectionStrategy, inject, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ThemeToggleComponent } from '../../components/theme-toggle/theme-toggle.component';
import { filter } from 'rxjs/operators';

export interface NavItem {
  icon: string;
  label: string;
  route: string;
  badge?: string;
}

export interface NavSection {
  label?: string;
  items: NavItem[];
}

const SIDEBAR_STORAGE_KEY = 'aegis-sidebar-collapsed';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, MatListModule, MatIconModule, MatButtonModule, MatTooltipModule, ThemeToggleComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent {
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  readonly collapsed = signal<boolean>(this.loadCollapsedState());
  readonly currentRoute = signal<string>(this.router.url);

  readonly sections: NavSection[] = [
    {
      label: 'Main',
      items: [
        { icon: 'dashboard', label: 'Dashboard', route: '/dashboard' },
        { icon: 'account_balance_wallet', label: 'Wallets', route: '/wallets' },
        { icon: 'payments', label: 'Payments', route: '/payments' },
      ],
    },
    {
      label: 'Management',
      items: [
        { icon: 'people', label: 'Users', route: '/users' },
        { icon: 'receipt_long', label: 'Transactions', route: '/transactions' },
      ],
    },
    {
      label: 'System',
      items: [
        { icon: 'settings', label: 'Settings', route: '/settings' },
      ],
    },
  ];

  readonly allItems = computed<NavItem[]>(() =>
    this.sections.flatMap((s) => s.items)
  );

  constructor() {
    this.router.events
      .pipe(filter((e) => e.constructor.name === 'NavigationEnd'), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.currentRoute.set(this.router.url);
      });
  }

  isActive(route: string): boolean {
    return this.router.url === route || this.router.url.startsWith(route + '/');
  }

  toggle(): void {
    this.collapsed.set(!this.collapsed());
    this.saveCollapsedState();
  }

  navigate(route: string): void {
    this.router.navigate([route]);
  }

  private loadCollapsedState(): boolean {
    if (typeof localStorage === 'undefined') return false;
    const stored = localStorage.getItem(SIDEBAR_STORAGE_KEY);
    return stored === 'true';
  }

  private saveCollapsedState(): void {
    if (typeof localStorage === 'undefined') return;
    localStorage.setItem(SIDEBAR_STORAGE_KEY, String(this.collapsed()));
  }
}
