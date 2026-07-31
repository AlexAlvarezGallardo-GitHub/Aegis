import { Directive, ElementRef, inject, input, effect, OnDestroy } from '@angular/core';

function isMotionReduced(): boolean {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function easeOut(t: number): number {
  return 1 - Math.pow(1 - t, 3);
}

function formatNumber(value: number, decimals: number): string {
  return value.toFixed(decimals);
}

@Directive({
  selector: '[appAegisCountUp]',
  standalone: true,
})
export class CountUpDirective implements OnDestroy {
  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly appAegisCountUp = input.required<number>();
  readonly duration = input(1000);
  readonly prefix = input('');
  readonly suffix = input('');
  readonly decimals = input(0);

  private observer: IntersectionObserver | null = null;
  private animated = false;

  ngOnDestroy(): void {
    this.observer?.disconnect();
    this.observer = null;
  }

  constructor() {
    effect(() => {
      const target = this.appAegisCountUp();
      if (!this.animated && !isMotionReduced()) {
        this.el.nativeElement.textContent = this.prefix() + formatNumber(0, this.decimals()) + this.suffix();
        this.setupObserver();
      } else {
        this.el.nativeElement.textContent = this.prefix() + formatNumber(target, this.decimals()) + this.suffix();
      }
    });
  }

  private setupObserver(): void {
    if (typeof window === 'undefined') return;

    this.observer = new IntersectionObserver((entries) => {
      if (entries[0]?.isIntersecting) {
        this.animate();
        this.observer?.disconnect();
      }
    }, { threshold: 0.3 });

    this.observer.observe(this.el.nativeElement);
  }

  private animate(): void {
    this.animated = true;
    const target = this.appAegisCountUp();
    const duration = this.duration();
    const decimals = this.decimals();
    const prefix = this.prefix();
    const suffix = this.suffix();
    const start = performance.now();

    const tick = (now: number): void => {
      const elapsed = now - start;
      const progress = Math.min(elapsed / duration, 1);
      const eased = easeOut(progress);
      const current = eased * target;

      this.el.nativeElement.textContent = prefix + formatNumber(current, decimals) + suffix;

      if (progress < 1) {
        requestAnimationFrame(tick);
      }
    };

    requestAnimationFrame(tick);
  }
}
