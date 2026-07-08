import { Injectable, signal } from '@angular/core';

export interface CommandItem {
  id: string;
  label: string;
  icon?: string;
  section: 'Navigation' | 'Actions';
  action: () => void;
  keywords?: string[];
}

const STORAGE_KEY = 'aegis-command-recent';
const MAX_RECENT = 5;

function loadRecent(): string[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    // localStorage may be unavailable
    return [];
  }
}

function saveRecent(ids: string[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(ids));
  } catch {
    // localStorage may be unavailable
  }
}

function fuzzyScore(query: string, text: string): number {
  const q = query.toLowerCase();
  const t = text.toLowerCase();
  if (t === q) return 100;
  if (t.startsWith(q)) return 90;
  if (t.includes(q)) return 70;

  let qi = 0;
  for (let ti = 0; ti < t.length && qi < q.length; ti++) {
    if (t[ti] === q[qi]) qi++;
  }
  if (qi === q.length) return 50;

  const words = t.split(/\s+/);
  if (words.some((w) => w.startsWith(q))) return 40;
  if (words.some((w) => w.includes(q))) return 30;

  return 0;
}

@Injectable({ providedIn: 'root' })
export class CommandPaletteService {
  private readonly itemsSignal = signal<CommandItem[]>([]);
  private readonly recentIds = signal<string[]>(loadRecent());

  readonly isOpen = signal(false);

  readonly items = this.itemsSignal.asReadonly();

  register(item: CommandItem): void {
    this.itemsSignal.update((list) => {
      if (list.some((i) => i.id === item.id)) return list;
      return [...list, item];
    });
  }

  unregister(item: CommandItem): void {
    this.itemsSignal.update((list) => list.filter((i) => i.id !== item.id));
  }

  search(query: string): { item: CommandItem; score: number }[] {
    if (!query.trim()) {
      const recent = this.recentIds();
      const recentItems = recent
        .map((id) => this.itemsSignal().find((i) => i.id === id))
        .filter((i): i is CommandItem => i !== undefined)
        .slice(0, MAX_RECENT);

      return [
        ...this.itemsSignal()
          .filter((i) => i.section === 'Navigation')
          .sort((a, b) => a.label.localeCompare(b.label))
          .map((i) => ({ item: i, score: 80 })),
        ...recentItems
          .filter((i) => i.section !== 'Navigation')
          .map((i) => ({ item: i, score: 100 })),
      ];
    }

    const results: { item: CommandItem; score: number }[] = [];

    for (const item of this.itemsSignal()) {
      const texts = [item.label, ...(item.keywords ?? [])];
      let bestScore = 0;
      for (const text of texts) {
        const score = fuzzyScore(query, text);
        if (score > bestScore) bestScore = score;
      }
      if (bestScore > 0) {
        results.push({ item, score: bestScore });
      }
    }

    return results.sort((a, b) => b.score - a.score).slice(0, 10);
  }

  select(item: CommandItem): void {
    this.addToRecent(item.id);
    this.isOpen.set(false);
    item.action();
  }

  toggle(): void {
    this.isOpen.update((v) => !v);
  }

  open(): void {
    this.isOpen.set(true);
  }

  close(): void {
    this.isOpen.set(false);
  }

  private addToRecent(id: string): void {
    this.recentIds.update((prev) => {
      const filtered = prev.filter((i) => i !== id);
      const updated = [id, ...filtered].slice(0, MAX_RECENT);
      saveRecent(updated);
      return updated;
    });
  }
}
