import { Component, ChangeDetectionStrategy, signal, computed, inject, DestroyRef, HostListener, effect } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd, ChildrenOutletContexts } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { filter, map, startWith } from 'rxjs/operators';
import { trigger, transition, style, animate } from '@angular/animations';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { HeaderComponent } from '../header/header.component';
import { ToastContainerComponent } from '../../components/toast-container/toast-container.component';
import { KeyboardShortcutCheatSheetComponent } from '../../components/keyboard-shortcut-cheat-sheet/keyboard-shortcut-cheat-sheet.component';
import { KeyboardShortcutsService } from '../../services/keyboard-shortcuts.service';
import { CommandPaletteComponent } from '../../components/command-palette/command-palette.component';
import { CommandPaletteService } from '../../services/command-palette.service';
import { MOBILE_BREAKPOINT } from '../../utils/breakpoints';

const isMotionReduced = (): boolean =>
  typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

const reducedMotionTransition = isMotionReduced()
  ? []
  : [
      style({ opacity: 0, transform: 'translateY(8px)' }),
      animate('200ms ease-out', style({ opacity: 1, transform: 'translateY(0)' })),
    ];

export const routeAnimation = trigger('routeAnimation', [
  transition('* => *', reducedMotionTransition.length > 0
    ? reducedMotionTransition
    : [style({ opacity: 1 })]
  ),
]);

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, RouterOutlet, SidebarComponent, HeaderComponent, ToastContainerComponent, KeyboardShortcutCheatSheetComponent, CommandPaletteComponent],
  animations: [routeAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss',
})
export class AppShellComponent {
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private keyboardShortcutsService = inject(KeyboardShortcutsService);
  private commandPaletteService = inject(CommandPaletteService);
  private contexts = inject(ChildrenOutletContexts);

  readonly isMobile = signal<boolean>(this.checkMobile());
  readonly sidebarCollapsed = signal<boolean>(false);
  readonly mobileOpen = signal<boolean>(false);

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map(() => this.router.url),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );

  readonly showShell = computed<boolean>(() => {
    const url = this.currentUrl();
    return url !== '/login' && url !== '/register' && !url.startsWith('/register');
  });

  constructor() {
    // Close the mobile drawer on navigation; never touch the desktop rail state.
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.isMobile()) {
          this.mobileOpen.set(false);
        }
      });

    // Lock body scroll while the mobile drawer is open.
    effect(() => {
      if (this.isMobile() && this.mobileOpen()) {
        document.body.style.overflow = 'hidden';
      } else {
        document.body.style.overflow = '';
      }
    });

    this.initKeyboardShortcuts();
  }

  @HostListener('window:resize')
  onResize(): void {
    this.isMobile.set(this.checkMobile());
    if (!this.checkMobile()) {
      this.mobileOpen.set(false);
    }
  }

  @HostListener('window:keydown.escape')
  onEscape(): void {
    if (this.isMobile() && this.mobileOpen()) {
      this.mobileOpen.set(false);
    }
  }

  private checkMobile(): boolean {
    return typeof window !== 'undefined' && window.innerWidth < MOBILE_BREAKPOINT;
  }

  private initKeyboardShortcuts(): void {
    const nav = (path: string) => this.router.navigate([path]);

    this.keyboardShortcutsService.register({
      keys: 'Cmd/Ctrl+k',
      description: 'Open command palette',
      action: () => this.commandPaletteService.toggle(),
    });

    this.initCommandPaletteItems(nav);
  }

  private initCommandPaletteItems(nav: (path: string) => void): void {
    this.commandPaletteService.register({
      id: 'nav-dashboard',
      label: 'Dashboard',
      icon: 'dashboard',
      section: 'Navigation',
      action: () => nav('/dashboard'),
    });
    this.commandPaletteService.register({
      id: 'nav-wallets',
      label: 'Wallets',
      icon: 'account_balance_wallet',
      section: 'Navigation',
      action: () => nav('/wallets'),
    });
    this.commandPaletteService.register({
      id: 'nav-payments',
      label: 'Payments',
      icon: 'payments',
      section: 'Navigation',
      action: () => nav('/payments'),
    });

    this.keyboardShortcutsService.register({
      keys: 'G then D',
      description: 'Navigate to Dashboard',
      action: () => nav('/dashboard'),
    });

    this.keyboardShortcutsService.register({
      keys: 'G then W',
      description: 'Navigate to Wallets',
      action: () => nav('/wallets'),
    });

    this.keyboardShortcutsService.register({
      keys: 'G then P',
      description: 'Navigate to Payments',
      action: () => nav('/payments'),
    });
  }

  getRouteAnimation(): string {
    return this.contexts.getContext('primary')?.route?.snapshot?.data?.['animation'] ?? '';
  }

  toggleSidebar(): void {
    if (this.isMobile()) {
      this.mobileOpen.update((open) => !open);
    } else {
      this.sidebarCollapsed.update((collapsed) => !collapsed);
    }
  }

  closeMobile(): void {
    this.mobileOpen.set(false);
  }

  onSidebarCollapsedChange(collapsed: boolean): void {
    this.sidebarCollapsed.set(collapsed);
  }
}
