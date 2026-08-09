import { Component, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { A11yModule } from '@angular/cdk/a11y';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { WalletResponse } from '../../../shared/models/wallet.model';
import { LoadingButtonComponent, ButtonVariant } from '../../../shared/forms/loading-button/loading-button.component';
import { FormFieldErrorComponent } from '../../../shared/forms/form-field-error/form-field-error.component';
import { formatMoney } from '../../../shared/utils/currency.pipe';

export type MoneyOperationMode = 'deposit' | 'withdraw' | 'adjust';

export interface MoneyDialogData {
  mode: MoneyOperationMode;
  wallet: WalletResponse;
}

export interface MoneyDialogResult {
  amount: number;
  source?: string;
  reference?: string;
  reason?: string;
}

@Component({
  selector: 'app-money-dialog',
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
  templateUrl: './money-dialog.component.html',
  styleUrl: './money-dialog.component.scss',
})
export class MoneyDialogComponent {
  private readonly dialogRef = inject<DialogRef<MoneyDialogResult>>(DialogRef);
  readonly data: MoneyDialogData = inject(DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly isDeposit = this.data.mode === 'deposit';
  readonly isWithdraw = this.data.mode === 'withdraw';
  readonly isAdjust = this.data.mode === 'adjust';

  readonly title = this.isDeposit ? 'Deposit Funds' : this.isWithdraw ? 'Withdraw Funds' : 'Adjust Balance';
  readonly confirmLabel = this.isDeposit ? 'Deposit' : this.isWithdraw ? 'Withdraw' : 'Adjust Balance';
  readonly confirmVariant: ButtonVariant = this.isDeposit ? 'primary' : 'danger';

  readonly fieldLabels: Record<string, string> = {
    amount: 'Amount',
    source: 'Source',
    reference: 'Reference',
    reason: 'Reason',
  };

  readonly form: FormGroup;

  readonly resultLabel = computed<string>(() => {
    const amount = parseFloat(this.form.get('amount')?.value);
    if (isNaN(amount) || amount <= 0) return '';
    return formatMoney(amount, this.data.wallet.currency);
  });

  constructor() {
    this.form = this.fb.group({
      amount: ['', [Validators.required, Validators.min(0.01)]],
      ...(this.isDeposit
        ? { source: ['', Validators.required], reference: ['', Validators.required] }
        : {}),
      ...(this.isWithdraw ? { reason: [''] } : {}),
      ...(this.isAdjust
        ? { reason: ['', Validators.required], reference: ['', Validators.required] }
        : {}),
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  submit(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    this.dialogRef.close({
      amount: parseFloat(v.amount),
      source: v.source ?? undefined,
      reference: v.reference ?? undefined,
      reason: v.reason ?? undefined,
    } satisfies MoneyDialogResult);
  }

  onOverlayKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      this.cancel();
    }
  }
}
