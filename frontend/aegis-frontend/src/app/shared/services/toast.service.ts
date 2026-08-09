import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastAction {
  label: string;
  callback: () => void;
  kind?: 'primary' | 'ghost';
}

export interface ToastOptions {
  description?: string;
  metadata?: string;
  action?: ToastAction;
  duration?: number;
  dismissible?: boolean;
}

export interface Toast {
  id: number;
  type: ToastType;
  title: string;
  description?: string;
  metadata?: string;
  action?: ToastAction;
  duration: number;
  dismissible: boolean;
}

const MAX_VISIBLE = 3;

const DEFAULT_DURATION: Record<ToastType, number> = {
  success: 4000,
  info: 4500,
  warning: 6000,
  error: 7000,
};

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly toastsSignal = signal<Toast[]>([]);
  private nextId = 0;

  readonly toasts = this.toastsSignal.asReadonly();

  private addToast(type: ToastType, title: string, opts: ToastOptions = {}): void {
    const id = this.nextId++;
    const toast: Toast = {
      id,
      type,
      title,
      description: opts.description,
      metadata: opts.metadata,
      action: opts.action,
      duration: opts.duration ?? DEFAULT_DURATION[type],
      dismissible: opts.dismissible ?? true,
    };
    // Stack with a hard cap of 3 visible notifications (oldest are dropped).
    this.toastsSignal.update((list) => [...list, toast].slice(-MAX_VISIBLE));

    if (toast.duration > 0) {
      setTimeout(() => this.dismiss(id), toast.duration);
    }
  }

  success(title: string, opts?: ToastOptions): void {
    this.addToast('success', title, opts);
  }

  error(title: string, opts?: ToastOptions): void {
    this.addToast('error', title, opts);
  }

  warning(title: string, opts?: ToastOptions): void {
    this.addToast('warning', title, opts);
  }

  info(title: string, opts?: ToastOptions): void {
    this.addToast('info', title, opts);
  }

  dismiss(id: number): void {
    this.toastsSignal.update((list) => list.filter((t) => t.id !== id));
  }

  dismissAll(): void {
    this.toastsSignal.set([]);
  }
}
