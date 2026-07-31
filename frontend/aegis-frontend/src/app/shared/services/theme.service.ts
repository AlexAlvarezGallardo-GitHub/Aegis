import { Injectable, inject, signal, computed, effect, PLATFORM_ID, DestroyRef } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type ThemeMode = 'light' | 'dark' | 'system';

const STORAGE_KEY = 'aegis-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly destroyRef = inject(DestroyRef);

  private readonly preference = signal<ThemeMode>(this.loadPreference());
  private readonly systemDark = signal<boolean>(this.getSystemPreference());

  readonly activeTheme = computed<'light' | 'dark'>(() => {
    const pref = this.preference();
    if (pref === 'system') {
      return this.systemDark() ? 'dark' : 'light';
    }
    return pref;
  });

  readonly isDark = computed(() => this.activeTheme() === 'dark');

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      const onChange = (e: MediaQueryListEvent): void => this.systemDark.set(e.matches);
      mediaQuery.addEventListener('change', onChange);

      this.destroyRef.onDestroy(() => {
        mediaQuery.removeEventListener('change', onChange);
      });

      effect(() => {
        this.applyTheme(this.activeTheme());
      });

      this.applyTheme(this.activeTheme());
    }
  }

  setPreference(mode: ThemeMode): void {
    this.preference.set(mode);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(STORAGE_KEY, mode);
    }
  }

  getPreference(): ThemeMode {
    return this.preference();
  }

  toggle(): void {
    this.setPreference(this.isDark() ? 'light' : 'dark');
  }

  private loadPreference(): ThemeMode {
    if (!isPlatformBrowser(this.platformId)) return 'system';
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'light' || stored === 'dark' || stored === 'system') {
      return stored;
    }
    return 'system';
  }

  private getSystemPreference(): boolean {
    if (!isPlatformBrowser(this.platformId)) return true;
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }

  private applyTheme(theme: 'light' | 'dark'): void {
    if (!isPlatformBrowser(this.platformId)) return;
    document.documentElement.setAttribute('data-theme', theme);
  }
}
