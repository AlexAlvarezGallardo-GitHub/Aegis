import { TestBed } from '@angular/core/testing';
import { KeyboardShortcutsService, Shortcut } from './keyboard-shortcuts.service';

describe('KeyboardShortcutsService', () => {
  let service: KeyboardShortcutsService;
  let actionSpy: jasmine.Spy;

  function createKeydown(key: string, options: { metaKey?: boolean; ctrlKey?: boolean; shiftKey?: boolean } = {}): KeyboardEvent {
    return new KeyboardEvent('keydown', { key, ...options, bubbles: true });
  }

  function dispatch(key: string, options?: { metaKey?: boolean; ctrlKey?: boolean; shiftKey?: boolean }): void {
    document.dispatchEvent(createKeydown(key, options));
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [KeyboardShortcutsService] });
    service = TestBed.inject(KeyboardShortcutsService);
    actionSpy = jasmine.createSpy('action');
  });

  afterEach(() => {
    service.unregisterAll();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should register and trigger a single-key shortcut', () => {
    service.register({ keys: 'B', description: 'Test', action: actionSpy });
    dispatch('B');
    expect(actionSpy).toHaveBeenCalled();
  });

  it('should not trigger shortcut when inside an input', () => {
    const input = document.createElement('input');
    document.body.appendChild(input);
    input.focus();

    service.register({ keys: '?', description: 'Test', action: actionSpy });
    input.dispatchEvent(createKeydown('?'));
    expect(actionSpy).not.toHaveBeenCalled();
    document.body.removeChild(input);
  });

  it('should trigger shortcut in input when allowInInput is true', () => {
    const input = document.createElement('input');
    document.body.appendChild(input);
    input.focus();

    service.register({ keys: '?', description: 'Test', action: actionSpy, allowInInput: true });
    input.dispatchEvent(createKeydown('?'));
    expect(actionSpy).toHaveBeenCalled();
    document.body.removeChild(input);
  });

  it('should handle modifier key shortcut (Cmd/Ctrl+K)', () => {
    service.register({ keys: 'Cmd/Ctrl+k', description: 'Test', action: actionSpy });
    dispatch('k', { metaKey: true });
    expect(actionSpy).toHaveBeenCalled();
  });

  it('should handle Ctrl key as modifier', () => {
    service.register({ keys: 'Cmd/Ctrl+k', description: 'Test', action: actionSpy });
    dispatch('k', { ctrlKey: true });
    expect(actionSpy).toHaveBeenCalled();
  });

  it('should register and trigger sequential key shortcut', () => {
    service.register({ keys: 'G then D', description: 'Test', action: actionSpy });
    dispatch('G');
    dispatch('D');
    expect(actionSpy).toHaveBeenCalled();
  });

  it('should timeout sequential key after 500ms', (done) => {
    service.register({ keys: 'G then D', description: 'Test', action: actionSpy });
    dispatch('G');
    setTimeout(() => {
      dispatch('D');
      expect(actionSpy).not.toHaveBeenCalled();
      done();
    }, 600);
  });

  it('should toggle cheat sheet on ? key', () => {
    expect(service.showCheatSheet()).toBeFalse();
    dispatch('?');
    expect(service.showCheatSheet()).toBeTrue();
    dispatch('?');
    expect(service.showCheatSheet()).toBeFalse();
  });

  it('should close cheat sheet on Escape', () => {
    service.showCheatSheet.set(true);
    dispatch('Escape');
    expect(service.showCheatSheet()).toBeFalse();
  });

  it('should unregister a shortcut', () => {
    const shortcut: Shortcut = { keys: '?', description: 'Test', action: actionSpy };
    service.register(shortcut);
    service.unregister(shortcut);
    dispatch('?');
    expect(actionSpy).not.toHaveBeenCalled();
  });

  it('should unregister all shortcuts', () => {
    service.register({ keys: 'A', description: 'Test 1', action: jasmine.createSpy('s1') });
    service.register({ keys: 'B', description: 'Test 2', action: jasmine.createSpy('s2') });
    service.unregisterAll();
    dispatch('A');
    dispatch('B');
    expect(actionSpy).not.toHaveBeenCalled();
  });

  it('should provide default shortcut groups', () => {
    expect(service.defaultShortcuts.length).toBeGreaterThan(0);
    const allKeys = service.defaultShortcuts.flatMap((g) => g.shortcuts).map((s) => s.keys);
    expect(allKeys).toContain('G then D');
    expect(allKeys).toContain('Cmd/Ctrl+K');
    expect(allKeys).toContain('?');
    expect(allKeys).toContain('Escape');
  });

  it('should detect platform and provide modifier key label', () => {
    expect(service.modifierKey).toMatch(/^(Cmd|Ctrl)$/);
  });
});
