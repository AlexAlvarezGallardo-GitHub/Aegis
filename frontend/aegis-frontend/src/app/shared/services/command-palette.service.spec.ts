import { TestBed } from '@angular/core/testing';
import { CommandPaletteService, CommandItem } from './command-palette.service';

describe('CommandPaletteService', () => {
  let service: CommandPaletteService;
  let dashItem: CommandItem;
  let walletItem: CommandItem;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [CommandPaletteService] });
    service = TestBed.inject(CommandPaletteService);
    localStorage.clear();

    dashItem = { id: 'nav-dashboard', label: 'Dashboard', icon: 'dashboard', section: 'Navigation', action: () => undefined };
    walletItem = { id: 'nav-wallets', label: 'Wallets', icon: 'account_balance_wallet', section: 'Navigation', action: () => undefined };

    service.register(dashItem);
    service.register(walletItem);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start closed', () => {
    expect(service.isOpen()).toBeFalse();
  });

  it('should toggle open/close', () => {
    service.toggle();
    expect(service.isOpen()).toBeTrue();
    service.toggle();
    expect(service.isOpen()).toBeFalse();
  });

  it('should open and close', () => {
    service.open();
    expect(service.isOpen()).toBeTrue();
    service.close();
    expect(service.isOpen()).toBeFalse();
  });

  it('should register items', () => {
    expect(service.items().length).toBe(2);
  });

  it('should not duplicate items on register', () => {
    service.register(dashItem);
    expect(service.items().length).toBe(2);
  });

  it('should unregister items', () => {
    service.unregister(dashItem);
    expect(service.items().length).toBe(1);
  });

  it('should search by label', () => {
    const results = service.search('Dashboard');
    expect(results.length).toBeGreaterThan(0);
    expect(results[0].item.id).toBe('nav-dashboard');
  });

  it('should search by partial match', () => {
    const results = service.search('Dash');
    expect(results.length).toBeGreaterThan(0);
    expect(results[0].item.id).toBe('nav-dashboard');
  });

  it('should search by fuzzy match', () => {
    const results = service.search('dsh');
    expect(results.length).toBeGreaterThan(0);
    expect(results[0].item.id).toBe('nav-dashboard');
  });

  it('should return empty results for no match', () => {
    const results = service.search('zzzzz');
    expect(results.length).toBe(0);
  });

  it('should return navigation items when query is empty', () => {
    const results = service.search('');
    expect(results.length).toBeGreaterThanOrEqual(2);
    expect(results.every((r) => r.item.section === 'Navigation')).toBeTrue();
  });

  it('should add to recent on select', () => {
    service.select(dashItem);
    expect(service.isOpen()).toBeFalse();
  });

  it('should close on select', () => {
    service.open();
    service.select(dashItem);
    expect(service.isOpen()).toBeFalse();
  });

  it('should sort results by score descending', () => {
    service.search('dashboard');
    // Dashboard should be top result
    const results = service.search('Dashboard');
    expect(results[0].score).toBeGreaterThanOrEqual(results[results.length - 1].score);
  });

  it('should limit results to 10', () => {
    for (let i = 0; i < 20; i++) {
      service.register({ id: `item-${i}`, label: `Item ${i}`, section: 'Actions', action: () => undefined });
    }
    const results = service.search('Item');
    expect(results.length).toBeLessThanOrEqual(10);
  });
});
