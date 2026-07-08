import { TestBed, ComponentFixture } from '@angular/core/testing';
import { KeyboardShortcutCheatSheetComponent } from './keyboard-shortcut-cheat-sheet.component';
import { KeyboardShortcutsService } from '../../services/keyboard-shortcuts.service';

describe('KeyboardShortcutCheatSheetComponent', () => {
  let fixture: ComponentFixture<KeyboardShortcutCheatSheetComponent>;
  let component: KeyboardShortcutCheatSheetComponent;
  let service: KeyboardShortcutsService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KeyboardShortcutCheatSheetComponent],
      providers: [KeyboardShortcutsService],
    }).compileComponents();

    fixture = TestBed.createComponent(KeyboardShortcutCheatSheetComponent);
    component = fixture.componentInstance;
    service = TestBed.inject(KeyboardShortcutsService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not show cheat sheet when hidden', () => {
    service.showCheatSheet.set(false);
    fixture.detectChanges();
    const overlay = fixture.nativeElement.querySelector('.cheat-sheet-overlay');
    expect(overlay).toBeFalsy();
  });

  it('should show cheat sheet when visible', () => {
    service.showCheatSheet.set(true);
    fixture.detectChanges();
    const overlay = fixture.nativeElement.querySelector('.cheat-sheet-overlay');
    expect(overlay).toBeTruthy();
  });

  it('should display shortcut groups', () => {
    service.showCheatSheet.set(true);
    fixture.detectChanges();
    const groupTitles = fixture.nativeElement.querySelectorAll('.shortcut-group-title');
    expect(groupTitles.length).toBe(service.defaultShortcuts.length);
    expect(groupTitles[0].textContent).toContain('Navigation');
  });

  it('should close on backdrop click', () => {
    service.showCheatSheet.set(true);
    fixture.detectChanges();
    const overlay = fixture.nativeElement.querySelector('.cheat-sheet-overlay');
    overlay.click();
    fixture.detectChanges();
    expect(service.showCheatSheet()).toBeFalse();
  });

  it('should have role dialog with aria-modal', () => {
    service.showCheatSheet.set(true);
    fixture.detectChanges();
    const overlay = fixture.nativeElement.querySelector('.cheat-sheet-overlay');
    expect(overlay.getAttribute('role')).toBe('dialog');
    expect(overlay.getAttribute('aria-modal')).toBe('true');
  });

  it('should close via close button', () => {
    service.showCheatSheet.set(true);
    fixture.detectChanges();
    const closeBtn = fixture.nativeElement.querySelector('.cheat-sheet-panel button');
    closeBtn.click();
    fixture.detectChanges();
    expect(service.showCheatSheet()).toBeFalse();
  });

  it('should display kbd elements for shortcut keys', () => {
    service.showCheatSheet.set(true);
    fixture.detectChanges();
    const kbdElements = fixture.nativeElement.querySelectorAll('kbd');
    expect(kbdElements.length).toBeGreaterThan(0);
    expect(kbdElements[0].classList.contains('shortcut-keys')).toBeTrue();
  });
});
