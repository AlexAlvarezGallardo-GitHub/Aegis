import { TestBed } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [ToastService] });
    service = TestBed.inject(ToastService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start with no toasts', () => {
    expect(service.toasts().length).toBe(0);
  });

  it('should add a success toast', () => {
    service.success('Operation completed');
    const toasts = service.toasts();
    expect(toasts.length).toBe(1);
    expect(toasts[0].type).toBe('success');
    expect(toasts[0].message).toBe('Operation completed');
  });

  it('should add an error toast', () => {
    service.error('Something went wrong');
    const toasts = service.toasts();
    expect(toasts.length).toBe(1);
    expect(toasts[0].type).toBe('error');
  });

  it('should add a warning toast', () => {
    service.warning('Proceed with caution');
    const toasts = service.toasts();
    expect(toasts.length).toBe(1);
    expect(toasts[0].type).toBe('warning');
  });

  it('should add an info toast', () => {
    service.info('For your information');
    const toasts = service.toasts();
    expect(toasts.length).toBe(1);
    expect(toasts[0].type).toBe('info');
  });

  it('should assign incrementing ids', () => {
    service.success('First');
    service.error('Second');
    const toasts = service.toasts();
    expect(toasts[0].id).toBe(0);
    expect(toasts[1].id).toBe(1);
  });

  it('should use default duration of 5000', () => {
    service.success('Test');
    expect(service.toasts()[0].duration).toBe(5000);
  });

  it('should use provided duration', () => {
    service.info('Test', 10000);
    expect(service.toasts()[0].duration).toBe(10000);
  });

  it('should use duration of 0 for persistence', () => {
    service.warning('Sticky', 0);
    expect(service.toasts()[0].duration).toBe(0);
  });

  it('should dismiss a toast by id', () => {
    service.success('First');
    service.error('Second');
    service.dismiss(0);
    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0].id).toBe(1);
  });

  it('should dismiss all toasts', () => {
    service.success('First');
    service.error('Second');
    service.info('Third');
    service.dismissAll();
    expect(service.toasts().length).toBe(0);
  });

  it('should auto-dismiss after duration', (done) => {
    jasmine.clock().install();
    service.success('Auto dismiss', 100);
    expect(service.toasts().length).toBe(1);
    jasmine.clock().tick(101);
    expect(service.toasts().length).toBe(0);
    jasmine.clock().uninstall();
    done();
  });

  it('should accept an action callback', () => {
    const action = { label: 'Undo', callback: () => undefined };
    service.success('Deleted', 5000, action);
    expect(service.toasts()[0].action).toEqual(action);
  });
});
