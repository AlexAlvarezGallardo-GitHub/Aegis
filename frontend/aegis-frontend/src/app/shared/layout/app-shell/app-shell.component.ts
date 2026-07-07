import { Component, ChangeDetectionStrategy, signal, computed, inject, DestroyRef, HostListener, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { MatSidenavModule, MatSidenav } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { filter } from 'rxjs/operators';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { HeaderComponent } from '../header/header.component';

const MOBILE_BREAKPOINT = 768;

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, MatSidenavModule, MatButtonModule, MatIconModule, RouterOutlet, SidebarComponent, HeaderComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss',
})
export class AppShellComponent {
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

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
  }

  @HostListener('window:resize')
  onResize(): void {
    this.isMobile.set(this.checkMobile());
  }

  private checkMobile(): boolean {
    return typeof window !== 'undefined' && window.innerWidth < MOBILE_BREAKPOINT;
  }

  toggleSidenav(): void {
    this.sidenav?.toggle();
  }
}
