import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { A11yModule } from '@angular/cdk/a11y';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { WalletResponse } from '../../../shared/models/wallet.model';
import { ToastService } from '../../../shared/services/toast.service';

export interface TechnicalDetailsData {
  wallet: WalletResponse;
}

@Component({
  selector: 'app-technical-details-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, A11yModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './technical-details-dialog.component.html',
  styleUrl: './technical-details-dialog.component.scss',
})
export class TechnicalDetailsDialogComponent {
  private readonly dialogRef = inject<DialogRef>(DialogRef);
  private readonly toastService = inject(ToastService);
  readonly data: TechnicalDetailsData = inject(DIALOG_DATA);

  close(): void {
    this.dialogRef.close();
  }

  copyId(): void {
    navigator.clipboard?.writeText(this.data.wallet.walletId).then(
      () => this.toastService.info('Wallet ID copied'),
      () => undefined,
    );
  }

  onOverlayKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      this.close();
    }
  }
}
