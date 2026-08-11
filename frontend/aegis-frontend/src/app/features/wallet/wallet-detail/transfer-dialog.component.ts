import { Component, ChangeDetectionStrategy, inject, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { A11yModule } from '@angular/cdk/a11y';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { WalletResponse } from '../../../shared/models/wallet.model';
import { LoadingButtonComponent } from '../../../shared/forms/loading-button/loading-button.component';
import { FormFieldErrorComponent } from '../../../shared/forms/form-field-error/form-field-error.component';
import { formatMoney } from '../../../shared/utils/currency.pipe';

export interface TransferDialogData {
  wallet: WalletResponse;
}

export interface TransferDialogResult {
  destWalletId: string;
  amount: number;
  reference: string;
  description?: string;
}

@Component({
  selector: 'app-transfer-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    A11yModule,
    LoadingButtonComponent,
    FormFieldErrorComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './transfer-dialog.component.html',
  styleUrl: './transfer-dialog.component.scss',
})
export class TransferDialogComponent {
  private readonly dialogRef = inject<DialogRef<TransferDialogResult>>(DialogRef);
  readonly data: TransferDialogData = inject(DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly title = 'Transfer Funds';
  readonly confirmLabel = 'Send Transfer';

  readonly fieldLabels: Record<string, string> = {
    destWalletId: 'Destination Wallet ID',
    amount: 'Amount',
    reference: 'Reference',
    description: 'Description',
  };

  readonly form: FormGroup;

  readonly defaultReference = `TRX-${Date.now()}`;

  // amountValue is a signal so the computed resultLabel re-evaluates on input changes.
  readonly amountValue = signal<number | null>(null);

  readonly resultLabel = computed<string>(() => {
    const amount = this.amountValue();
    if (amount === null || isNaN(amount) || amount <= 0) return '';
    return formatMoney(amount, this.data.wallet.currency);
  });

  constructor() {
    this.form = this.fb.group({
      destWalletId: ['', [Validators.required, Validators.minLength(8)]],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      reference: [this.defaultReference, [Validators.required]],
      description: [''],
    });
    this.form.get('amount')?.valueChanges.subscribe((v) => {
      const parsed = parseFloat(v);
      this.amountValue.set(isNaN(parsed) ? null : parsed);
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  submit(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    this.dialogRef.close({
      destWalletId: v.destWalletId,
      amount: parseFloat(v.amount),
      reference: v.reference,
      description: v.description || undefined,
    } satisfies TransferDialogResult);
  }

  onOverlayKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      this.cancel();
    }
  }
}
