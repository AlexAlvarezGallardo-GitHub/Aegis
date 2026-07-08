import { Component, ChangeDetectionStrategy, signal, computed, inject, DestroyRef, HostListener, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd, ChildrenOutletContexts } from '@angular/router';
import { MatSidenavModule, MatSidenav } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { filter } from 'rxjs/operators';
import { trigger, transition, style, animate } from '@angular/animations';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { HeaderComponent } from '../header/header.component';
import { ToastContainerComponent } from '../../components/toast-container/toast-container.component';
import { KeyboardShortcutCheatSheetComponent } from '../../components/keyboard-shortcut-cheat-sheet/keyboard-shortcut-cheat-sheet.component';
import { KeyboardShortcutsService } from '../../services/keyboard-shortcuts.service';
import { CommandPaletteComponent } from '../../components/command-palette/command-palette.component';
import { CommandPaletteService } from '../../services/command-palette.service';

const isMotionReduced = (): boolean =>
  typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

const reducedMotionTransition = isMotionReduced()
  ? []
  : [
      style({ opacity: 0, transform: 'translateX(20px)' }),
      animate('200ms ease-out', style({ opacity: 1, transform: 'translateX(0)' })),
    ];

export const routeAnimation = trigger('routeAnimation', [
  transition('* => *', reducedMotionTransition.length > 0
    ? reducedMotionTransition
    : [style({ opacity: 1 })]
  ),
]);

const MOBILE_BREAKPOINT = 768;

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, MatSidenavModule, MatButtonModule, MatIconModule, RouterOutlet, SidebarComponent, HeaderComponent, ToastContainerComponent, KeyboardShortcutCheatSheetComponent, CommandPaletteComponent],
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

  @ViewChild('sidenav') sidenav!: MatSidenav;

  readonly isMobile = signal<boolean>(this.checkMobile());

  readonly showShell = computed<boolean>(() => {
    const url = this.router.url;
    return url !== '/login' && url !== '/register' && !url.startsWith('/register');
  });

  constructor() {
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.isMobile()) {
          this.sidenav?.close();
        }
      });

    this.initKeyboardShortcuts();
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

  @HostListener('window:resize')
  onResize(): void {
    this.isMobile.set(this.checkMobile());
  }

  private checkMobile(): boolean {
    return typeof window !== 'undefined' && window.innerWidth < MOBILE_BREAKPOINT;
  }

  getRouteAnimation(): string {
    return this.contexts.getContext('primary')?.route?.snapshot?.data?.['animation'] ?? '';
  }

  toggleSidenav(): void {
    this.sidenav?.toggle();
  }
}
