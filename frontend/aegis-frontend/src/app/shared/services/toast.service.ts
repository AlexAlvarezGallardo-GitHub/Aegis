import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastAction {
  label: string;
  callback: () => void;
}

export interface Toast {
  id: number;
  type: ToastType;
  message: string;
  duration: number;
  action?: ToastAction;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly toastsSignal = signal<Toast[]>([]);
  private nextId = 0;

  readonly toasts = this.toastsSignal.asReadonly();

  private addToast(type: ToastType, message: string, duration?: number, action?: ToastAction): void {
    const id = this.nextId++;
    const toast: Toast = { id, type, message, duration: duration ?? 5000, action };
    this.toastsSignal.update((list) => [...list, toast]);

    if (toast.duration > 0) {
      setTimeout(() => this.dismiss(id), toast.duration);
    }
  }

  success(message: string, duration?: number, action?: ToastAction): void {
    this.addToast('success', message, duration, action);
  }

  error(message: string, duration?: number, action?: ToastAction): void {
    this.addToast('error', message, duration, action);
  }

  warning(message: string, duration?: number, action?: ToastAction): void {
    this.addToast('warning', message, duration, action);
  }

  info(message: string, duration?: number, action?: ToastAction): void {
    this.addToast('info', message, duration, action);
  }

  dismiss(id: number): void {
    this.toastsSignal.update((list) => list.filter((t) => t.id !== id));
  }

  dismissAll(): void {
    this.toastsSignal.set([]);
  }
}
