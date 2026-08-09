import { Component, ChangeDetectionStrategy, inject, signal, input, output, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { filter } from 'rxjs/operators';

export interface NavItem {
  icon: string;
  label: string;
  route: string;
}

export interface NavSection {
  label?: string;
  items: NavItem[];
}

const SIDEBAR_STORAGE_KEY = 'aegis-sidebar-collapsed';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule, MatButtonModule, MatTooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent {
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  readonly collapsed = input<boolean>(this.loadCollapsedState());
  readonly isMobile = input<boolean>(false);
  readonly mobileOpen = input<boolean>(false);
  readonly collapsedChange = output<boolean>();
  readonly mobileClose = output<void>();

  readonly currentRoute = signal<string>(this.router.url);

  readonly sections: NavSection[] = [
    {
      label: 'Payments',
      items: [
        { icon: 'payments', label: 'Payments', route: '/payments' },
        { icon: 'receipt_long', label: 'Transactions', route: '/transactions' },
        { icon: 'account_balance', label: 'Payouts', route: '/payouts' },
      ],
    },
    {
      label: 'Wallets',
      items: [
        { icon: 'account_balance_wallet', label: 'Wallets', route: '/wallets' },
        { icon: 'currency_exchange', label: 'Currencies', route: '/currencies' },
      ],
    },
    {
      label: 'Monitoring',
      items: [
        { icon: 'shield', label: 'Fraud', route: '/fraud' },
        { icon: 'notifications_active', label: 'Alerts', route: '/alerts' },
        { icon: 'monitor_heart', label: 'System Health', route: '/health' },
      ],
    },
    {
      label: 'Settings',
      items: [
        { icon: 'settings', label: 'Settings', route: '/settings' },
        { icon: 'people', label: 'Users', route: '/users' },
        { icon: 'key', label: 'API Keys', route: '/api-keys' },
      ],
    },
  ];

  constructor() {
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.currentRoute.set(this.router.url);
      });
  }

  isActive(route: string): boolean {
    return this.router.url === route || this.router.url.startsWith(route + '/');
  }

  toggle(): void {
    this.collapsedChange.emit(!this.collapsed());
    this.saveCollapsedState(!this.collapsed());
  }

  close(): void {
    this.mobileClose.emit();
  }

  private loadCollapsedState(): boolean {
    if (typeof localStorage === 'undefined') return false;
    const stored = localStorage.getItem(SIDEBAR_STORAGE_KEY);
    return stored === 'true';
  }

  private saveCollapsedState(collapsed: boolean): void {
    if (typeof localStorage === 'undefined') return;
    localStorage.setItem(SIDEBAR_STORAGE_KEY, String(collapsed));
  }
}
