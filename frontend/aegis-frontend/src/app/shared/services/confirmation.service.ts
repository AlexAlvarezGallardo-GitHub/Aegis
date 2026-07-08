import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Dialog } from '@angular/cdk/dialog';
import {
  ConfirmationDialogComponent,
  ConfirmationData,
} from '../components/confirmation-dialog/confirmation-dialog.component';

export interface ConfirmationOptions {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  destructive?: boolean;
  disableClose?: boolean;
}

@Injectable({ providedIn: 'root' })
export class ConfirmationService {
  private readonly dialog = inject(Dialog);

  confirm(options: ConfirmationOptions): Observable<boolean> {
    const data: ConfirmationData = {
      title: options.title,
      message: options.message,
      confirmText: options.confirmText ?? 'Confirm',
      cancelText: options.cancelText ?? 'Cancel',
      destructive: options.destructive ?? false,
    };

    const dialogRef = this.dialog.open<boolean>(ConfirmationDialogComponent, {
      data,
      disableClose: options.disableClose ?? false,
      hasBackdrop: true,
      backdropClass: 'cdk-overlay-transparent-backdrop',
    });

    return dialogRef.closed.pipe(map((result) => result ?? false));
  }
}
