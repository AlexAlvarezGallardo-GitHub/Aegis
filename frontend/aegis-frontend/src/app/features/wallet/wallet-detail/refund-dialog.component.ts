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
import { AegisCurrencyPipe, formatMoney } from '../../../shared/utils/currency.pipe';

export interface RefundDialogData {
  wallet: WalletResponse;
  paymentId: string;
  paymentAmount: number;
}

export interface RefundDialogResult {
  amount?: number;
  reason?: string;
  reference: string;
}

@Component({
  selector: 'app-refund-dialog',
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
    AegisCurrencyPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './refund-dialog.component.html',
  styleUrl: './refund-dialog.component.scss',
})
export class RefundDialogComponent {
  private readonly dialogRef = inject<DialogRef<RefundDialogResult>>(DialogRef);
  readonly data: RefundDialogData = inject(DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly title = 'Refund Payment';
  readonly confirmLabel = 'Refund';

  readonly fieldLabels: Record<string, string> = {
    amount: 'Amount',
    reference: 'Reference',
    reason: 'Reason',
  };

  readonly form: FormGroup;

  readonly defaultReference = `REF-${Date.now()}`;

  // amountValue is a signal so the computed resultLabel re-evaluates on input changes.
  readonly amountValue = signal<number | null>(null);

  readonly resultLabel = computed<string>(() => {
    const amount = this.amountValue();
    if (amount === null || isNaN(amount) || amount <= 0) return '';
    return formatMoney(amount, this.data.wallet.currency);
  });

  constructor() {
    this.form = this.fb.group({
      amount: ['', [Validators.min(0.01)]],
      reference: [this.defaultReference, [Validators.required]],
      reason: [''],
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
    const amount = v.amount ? parseFloat(v.amount) : undefined;
    this.dialogRef.close({
      amount,
      reason: v.reason || undefined,
      reference: v.reference,
    } satisfies RefundDialogResult);
  }

  onOverlayKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      this.cancel();
    }
  }
}
