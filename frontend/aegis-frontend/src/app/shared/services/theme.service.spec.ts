import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [ThemeService] });
    service = TestBed.inject(ThemeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should default to system preference when no localStorage value', () => {
    const pref = service.getPreference();
    expect(pref).toBe('system');
  });

  it('should return dark when preference is set to dark', () => {
    service.setPreference('dark');
    expect(service.getPreference()).toBe('dark');
  });

  it('should return light when preference is set to light', () => {
    service.setPreference('light');
    expect(service.getPreference()).toBe('light');
  });

  it('should toggle from dark to light', () => {
    service.setPreference('dark');
    service.toggle();
    expect(service.getPreference()).toBe('light');
  });

  it('should toggle from light to dark', () => {
    service.setPreference('light');
    service.toggle();
    expect(service.getPreference()).toBe('dark');
  });

  it('should persist preference to localStorage', () => {
    service.setPreference('dark');
    expect(localStorage.getItem('aegis-theme')).toBe('dark');
  });

  it('should set data-theme attribute on document', () => {
    service.setPreference('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('should restore preference from localStorage on init', () => {
    localStorage.setItem('aegis-theme', 'dark');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [ThemeService] });
    const restored = TestBed.inject(ThemeService);
    expect(restored.getPreference()).toBe('dark');
  });
});
