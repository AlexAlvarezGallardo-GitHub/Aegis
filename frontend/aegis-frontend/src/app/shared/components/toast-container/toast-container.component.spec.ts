import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ToastContainerComponent } from './toast-container.component';
import { ToastService } from '../../services/toast.service';

describe('ToastContainerComponent', () => {
  let fixture: ComponentFixture<ToastContainerComponent>;
  let component: ToastContainerComponent;
  let toastService: ToastService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToastContainerComponent],
      providers: [ToastService],
    }).compileComponents();

    fixture = TestBed.createComponent(ToastContainerComponent);
    component = fixture.componentInstance;
    toastService = TestBed.inject(ToastService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show no toasts initially', () => {
    const toasts = fixture.nativeElement.querySelectorAll('.toast');
    expect(toasts.length).toBe(0);
  });

  it('should render toasts from the service', () => {
    toastService.success('Success!');
    toastService.error('Error!');
    fixture.detectChanges();

    const toasts = fixture.nativeElement.querySelectorAll('.toast');
    expect(toasts.length).toBe(2);
  });

  it('should render correct messages', () => {
    toastService.success('All good');
    toastService.error('Bad thing');
    fixture.detectChanges();

    const messages = fixture.nativeElement.querySelectorAll('.toast-message');
    expect(messages[0].textContent).toContain('All good');
    expect(messages[1].textContent).toContain('Bad thing');
  });

  it('should apply correct CSS class for each type', () => {
    toastService.success('S');
    toastService.error('E');
    toastService.warning('W');
    toastService.info('I');
    fixture.detectChanges();

    const toasts = fixture.nativeElement.querySelectorAll('.toast');
    expect(toasts[0].classList.contains('toast-success')).toBeTrue();
    expect(toasts[1].classList.contains('toast-error')).toBeTrue();
    expect(toasts[2].classList.contains('toast-warning')).toBeTrue();
    expect(toasts[3].classList.contains('toast-info')).toBeTrue();
  });

  it('should dismiss toast on click', () => {
    toastService.success('Click to dismiss');
    fixture.detectChanges();
    expect(toastService.toasts().length).toBe(1);

    const toastEl = fixture.nativeElement.querySelector('.toast');
    toastEl.click();
    fixture.detectChanges();
    expect(toastService.toasts().length).toBe(0);
  });

  it('should render action button when present', () => {
    toastService.success('Deleted', 5000, { label: 'Undo', callback: () => undefined });
    fixture.detectChanges();

    const actionBtn = fixture.nativeElement.querySelector('.toast-action');
    expect(actionBtn).toBeTruthy();
    expect(actionBtn.textContent).toContain('Undo');
  });

  it('should have aria-live region', () => {
    const arena = fixture.nativeElement.querySelector('.toast-arena');
    expect(arena.getAttribute('aria-live')).toBe('polite');
  });

  it('should have role alert on each toast', () => {
    toastService.error('Alert!');
    fixture.detectChanges();

    const toast = fixture.nativeElement.querySelector('.toast');
    expect(toast.getAttribute('role')).toBe('alert');
  });
});
