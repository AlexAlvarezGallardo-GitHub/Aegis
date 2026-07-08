import { Component, ChangeDetectionStrategy, inject, signal, HostListener, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { CommandPaletteService } from '../../services/command-palette.service';

@Component({
  selector: 'app-command-palette',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './command-palette.component.html',
  styleUrl: './command-palette.component.scss',
})
export class CommandPaletteComponent implements AfterViewInit {
  readonly paletteService = inject(CommandPaletteService);

  @ViewChild('searchInput') searchInput?: ElementRef<HTMLInputElement>;

  readonly query = signal('');
  readonly selectedIndex = signal(0);

  readonly results = signal<{ item: import('../../services/command-palette.service').CommandItem; score: number }[]>([]);

  ngAfterViewInit(): void {
    if (this.paletteService.isOpen()) {
      this.searchInput?.nativeElement.focus();
    }
  }

  @HostListener('keydown.escape')
  onEscape(): void {
    this.paletteService.close();
  }

  onInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.query.set(value);
    this.selectedIndex.set(0);
    this.results.set(this.paletteService.search(value));
  }

  onKeydown(event: KeyboardEvent): void {
    const current = this.results();
    if (current.length === 0) return;

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.selectedIndex.update((i) => (i + 1) % current.length);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.selectedIndex.update((i) => (i - 1 + current.length) % current.length);
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const idx = this.selectedIndex();
      if (current[idx]) {
        this.paletteService.select(current[idx].item);
      }
    }
  }

  selectItem(item: import('../../services/command-palette.service').CommandItem): void {
    this.paletteService.select(item);
  }

  close(): void {
    this.paletteService.close();
  }
}
