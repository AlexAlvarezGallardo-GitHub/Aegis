import { Component, signal } from '@angular/core';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { CountUpDirective } from './count-up.directive';

@Component({
  template: `<span [appAegisCountUp]="value()" [duration]="duration()" [prefix]="prefix()" [suffix]="suffix()" [decimals]="decimals()"></span>`,
  imports: [CountUpDirective],
  standalone: true,
})
class TestHostComponent {
  readonly value = signal(1000);
  readonly duration = signal(9999999);
  readonly prefix = signal('');
  readonly suffix = signal('');
  readonly decimals = signal(0);
}

describe('CountUpDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let spanEl: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    spanEl = fixture.nativeElement.querySelector('span');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(spanEl).toBeTruthy();
  });

  it('should show final value immediately when reduced motion', () => {
    const originalMatchMedia = window.matchMedia;
    window.matchMedia = (query: string) =>
      ({ matches: query === '(prefers-reduced-motion: reduce)', addEventListener: () => undefined }) as unknown as MediaQueryList;

    const fixture2 = TestBed.createComponent(TestHostComponent);
    fixture2.detectChanges();
    const el = fixture2.nativeElement.querySelector('span');
    expect(el.textContent).toBe('1000');

    window.matchMedia = originalMatchMedia;
  });

  it('should apply prefix when reduced motion', () => {
    const originalMatchMedia = window.matchMedia;
    window.matchMedia = (query: string) =>
      ({ matches: query === '(prefers-reduced-motion: reduce)', addEventListener: () => undefined }) as unknown as MediaQueryList;

    component.prefix.set('$');
    fixture.detectChanges();
    expect(spanEl.textContent).toBe('$1000');

    window.matchMedia = originalMatchMedia;
  });

  it('should apply suffix when reduced motion', () => {
    const originalMatchMedia = window.matchMedia;
    window.matchMedia = (query: string) =>
      ({ matches: query === '(prefers-reduced-motion: reduce)', addEventListener: () => undefined }) as unknown as MediaQueryList;

    component.suffix.set('%');
    fixture.detectChanges();
    expect(spanEl.textContent).toBe('1000%');

    window.matchMedia = originalMatchMedia;
  });

  it('should handle decimals when reduced motion', () => {
    const originalMatchMedia = window.matchMedia;
    window.matchMedia = (query: string) =>
      ({ matches: query === '(prefers-reduced-motion: reduce)', addEventListener: () => undefined }) as unknown as MediaQueryList;

    component.decimals.set(2);
    component.value.set(1234.56);
    fixture.detectChanges();
    expect(spanEl.textContent).toBe('1234.56');

    window.matchMedia = originalMatchMedia;
  });

  it('should update when value input changes', () => {
    const originalMatchMedia = window.matchMedia;
    window.matchMedia = (query: string) =>
      ({ matches: query === '(prefers-reduced-motion: reduce)', addEventListener: () => undefined }) as unknown as MediaQueryList;

    component.value.set(2500);
    fixture.detectChanges();
    expect(spanEl.textContent).toBe('2500');

    window.matchMedia = originalMatchMedia;
  });
});
