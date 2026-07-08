import { TestBed, ComponentFixture } from '@angular/core/testing';
import { CommandPaletteComponent } from './command-palette.component';
import { CommandPaletteService, CommandItem } from '../../services/command-palette.service';

describe('CommandPaletteComponent', () => {
  let fixture: ComponentFixture<CommandPaletteComponent>;
  let component: CommandPaletteComponent;
  let service: CommandPaletteService;

  const navItem: CommandItem = {
    id: 'nav-test',
    label: 'Test Page',
    icon: 'test',
    section: 'Navigation',
    action: () => undefined,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommandPaletteComponent],
      providers: [CommandPaletteService],
    }).compileComponents();

    fixture = TestBed.createComponent(CommandPaletteComponent);
    component = fixture.componentInstance;
    service = TestBed.inject(CommandPaletteService);
    service.register(navItem);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not show when closed', () => {
    service.close();
    fixture.detectChanges();
    const overlay = fixture.nativeElement.querySelector('.palette-overlay');
    expect(overlay).toBeFalsy();
  });

  it('should show when open', () => {
    service.open();
    fixture.detectChanges();
    const overlay = fixture.nativeElement.querySelector('.palette-overlay');
    expect(overlay).toBeTruthy();
  });

  it('should display search results', () => {
    service.open();
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('.palette-input');
    input.value = 'Test';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const items = fixture.nativeElement.querySelectorAll('.palette-item');
    expect(items.length).toBe(1);
    expect(items[0].textContent).toContain('Test Page');
  });

  it('should show empty state when no results', () => {
    service.open();
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('.palette-input');
    input.value = 'zzzzz';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const empty = fixture.nativeElement.querySelector('.palette-empty');
    expect(empty).toBeTruthy();
    expect(empty.textContent).toContain('No results found');
  });

  it('should close on backdrop click', () => {
    service.open();
    fixture.detectChanges();
    const overlay = fixture.nativeElement.querySelector('.palette-overlay');
    overlay.click();
    fixture.detectChanges();
    expect(service.isOpen()).toBeFalse();
  });

  it('should have role dialog with aria-modal', () => {
    service.open();
    fixture.detectChanges();
    const overlay = fixture.nativeElement.querySelector('.palette-overlay');
    expect(overlay.getAttribute('role')).toBe('dialog');
    expect(overlay.getAttribute('aria-modal')).toBe('true');
  });

  it('should navigate results with arrow keys', () => {
    service.open();
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('.palette-input');
    input.value = 'Test';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.selectedIndex()).toBe(0);

    const downEvent = new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true });
    input.dispatchEvent(downEvent);
    fixture.detectChanges();
    expect(component.selectedIndex()).toBe(0); // only one item

    const upEvent = new KeyboardEvent('keydown', { key: 'ArrowUp', bubbles: true });
    input.dispatchEvent(upEvent);
    fixture.detectChanges();
    expect(component.selectedIndex()).toBe(0);
  });

  it('should select item on click', () => {
    const selectSpy = spyOn(service, 'select').and.callThrough();
    service.open();
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('.palette-input');
    input.value = 'Test';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const item = fixture.nativeElement.querySelector('.palette-item');
    item.click();
    fixture.detectChanges();

    expect(selectSpy).toHaveBeenCalledWith(navItem);
  });
});
