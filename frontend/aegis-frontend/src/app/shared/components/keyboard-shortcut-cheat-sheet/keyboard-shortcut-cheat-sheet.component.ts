import { Component, ChangeDetectionStrategy, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { KeyboardShortcutsService } from '../../services/keyboard-shortcuts.service';

@Component({
  selector: 'app-keyboard-shortcut-cheat-sheet',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './keyboard-shortcut-cheat-sheet.component.html',
  styleUrl: './keyboard-shortcut-cheat-sheet.component.scss',
})
export class KeyboardShortcutCheatSheetComponent {
  readonly shortcutsService = inject(KeyboardShortcutsService);

  @HostListener('keydown.escape')
  onEscape(): void {
    this.shortcutsService.showCheatSheet.set(false);
  }

  close(): void {
    this.shortcutsService.showCheatSheet.set(false);
  }
}
