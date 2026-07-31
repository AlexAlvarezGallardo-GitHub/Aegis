import { Injectable, signal, DestroyRef, inject } from '@angular/core';

export interface Shortcut {
  keys: string;
  description: string;
  action: () => void;
  allowInInput?: boolean;
}

export interface ShortcutGroup {
  group: string;
  shortcuts: Shortcut[];
}

const NOOP = (): void => undefined;

const DEFAULT_SHORTCUTS: ShortcutGroup[] = [
  {
    group: 'Navigation',
    shortcuts: [
      { keys: 'G then D', description: 'Navigate to Dashboard', action: NOOP },
      { keys: 'G then W', description: 'Navigate to Wallets', action: NOOP },
      { keys: 'G then P', description: 'Navigate to Payments', action: NOOP },
    ],
  },
  {
    group: 'Actions',
    shortcuts: [
      { keys: 'Cmd/Ctrl+K', description: 'Open command palette', action: NOOP },
      { keys: 'Cmd/Ctrl+/', description: 'Focus global search', action: NOOP },
      { keys: '?', description: 'Toggle keyboard shortcuts help', action: NOOP },
    ],
  },
  {
    group: 'General',
    shortcuts: [
      { keys: 'Escape', description: 'Close modal / dialog / command palette', action: NOOP },
    ],
  },
];

@Injectable({ providedIn: 'root' })
export class KeyboardShortcutsService {
  private readonly destroyRef = inject(DestroyRef);
  private readonly registeredShortcuts = signal<Map<string, Shortcut>>(new Map());
  private sequenceBuffer: { key: string; timer: ReturnType<typeof setTimeout> } | null = null;
  private readonly isMac = typeof navigator !== 'undefined' && navigator.platform.includes('Mac');

  readonly showCheatSheet = signal(false);

  readonly defaultShortcuts = DEFAULT_SHORTCUTS;
  readonly modifierKey = this.isMac ? 'Cmd' : 'Ctrl';

  constructor() {
    if (typeof document !== 'undefined') {
      document.addEventListener('keydown', this.onKeydown);
      this.destroyRef.onDestroy(() => {
        document.removeEventListener('keydown', this.onKeydown);
      });
    }
  }

  register(shortcut: Shortcut): void {
    this.registeredShortcuts.update((map) => {
      map.set(shortcut.keys, shortcut);
      return new Map(map);
    });
  }

  unregister(shortcut: Shortcut): void {
    this.registeredShortcuts.update((map) => {
      map.delete(shortcut.keys);
      return new Map(map);
    });
  }

  unregisterAll(): void {
    this.registeredShortcuts.set(new Map());
  }

  toggleCheatSheet(): void {
    this.showCheatSheet.update((v) => !v);
  }

  private onKeydown = (event: KeyboardEvent): void => {
    const target = event.target as HTMLElement | null;
    const isInput = target && typeof target.matches === 'function'
      && target.matches('input, textarea, select, [contenteditable]');

    if (event.key === 'Escape') {
      if (this.showCheatSheet()) {
        this.showCheatSheet.set(false);
        event.preventDefault();
        return;
      }
      return;
    }

    if (event.key === '?' && !event.metaKey && !event.ctrlKey && !isInput) {
      this.toggleCheatSheet();
      event.preventDefault();
      return;
    }

    if (!this.registeredShortcuts().size) return;

    const shortcut = this.matchShortcut(event);
    if (shortcut) {
      if (isInput && !shortcut.allowInInput) return;
      event.preventDefault();
      event.stopPropagation();
      shortcut.action();
    }
  };

  private matchShortcut(event: KeyboardEvent): Shortcut | undefined {
    const maps = this.registeredShortcuts();

    const hasMod = event.metaKey || event.ctrlKey;
    const key = event.key === '/' ? '/' : event.key;

    if (hasMod) {
      const modKey = this.isMac ? 'meta' : 'ctrl';

      const comboKeys = [
        `${modKey}+${key.toLowerCase()}`,
        `${modKey}+${key}`,
      ];

      if (event.shiftKey) {
        comboKeys.push(`${modKey}+shift+${key.toLowerCase()}`);
        comboKeys.push(`${modKey}+shift+${key}`);
      }

      for (const k of comboKeys) {
        const match = maps.get(k);
        if (match) return match;
      }

      const labelKeys = [
        `Cmd/Ctrl+${key.toLowerCase()}`,
        `Cmd/Ctrl+${key}`,
      ];

      if (event.shiftKey) {
        labelKeys.push(`Cmd/Ctrl+Shift+${key.toLowerCase()}`);
        labelKeys.push(`Cmd/Ctrl+Shift+${key}`);
      }

      for (const k of labelKeys) {
        const match = maps.get(k);
        if (match) return match;
      }

      return undefined;
    }

    const single = maps.get(key);
    if (single) return single;

    return this.matchSequence(event);
  }

  private matchSequence(event: KeyboardEvent): Shortcut | undefined {
    const key = event.key;

    if (this.sequenceBuffer) {
      clearTimeout(this.sequenceBuffer.timer);
      const prevKey = this.sequenceBuffer.key;
      this.sequenceBuffer = null;

      const seqKey = `${prevKey}.${key}`;
      const match = this.registeredShortcuts().get(seqKey);
      if (match) return match;

      const labelMatch = this.registeredShortcuts().get(`${prevKey} then ${key}`);
      if (labelMatch) return labelMatch;

      return undefined;
    }

    const hasSequence = this.hasSequenceStartingWith(key);
    if (hasSequence) {
      this.sequenceBuffer = {
        key,
        timer: setTimeout(() => {
          this.sequenceBuffer = null;
        }, 500),
      };
    }

    return undefined;
  }

  private hasSequenceStartingWith(key: string): boolean {
    const prefix1 = `${key}.`;
    const prefix2 = `${key} then `;
    for (const k of this.registeredShortcuts().keys()) {
      if (k.startsWith(prefix1) || k.startsWith(prefix2)) return true;
    }
    return false;
  }
}
