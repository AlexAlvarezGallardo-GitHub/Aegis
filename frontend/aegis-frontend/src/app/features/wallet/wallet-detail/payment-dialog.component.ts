import { Component, ChangeDetectionStrategy, inject, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { A11yModule } from '@angular/cdk/a11y';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { WalletResponse } from '../../../shared/models/wallet.model';
import { LoadingButtonComponent } from '../../../shared/forms/loading-button/loading-button.component';
import { FormFieldErrorComponent } from '../../../shared/forms/form-field-error/form-field-error.component';
import { formatMoney } from '../../../shared/utils/currency.pipe';

export interface PaymentDialogData {
  wallet: WalletResponse;
}

export interface PaymentDialogResult {
  payee: { name: string; id: string; type: 'MERCHANT' | 'INDIVIDUAL' | 'SERVICE' };
  amount: number;
  reference: string;
  description?: string;
}

@Component({
  selector: 'app-payment-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    A11yModule,
    LoadingButtonComponent,
    FormFieldErrorComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './payment-dialog.component.html',
  styleUrl: './payment-dialog.component.scss',
})
export class PaymentDialogComponent {
  private readonly dialogRef = inject<DialogRef<PaymentDialogResult>>(DialogRef);
  readonly data: PaymentDialogData = inject(DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly title = 'Make a Payment';
  readonly confirmLabel = 'Pay';

  readonly payeeTypes = ['MERCHANT', 'INDIVIDUAL', 'SERVICE'] as const;

  readonly fieldLabels: Record<string, string> = {
    payeeName: 'Payee Name',
    payeeId: 'Payee ID',
    payeeType: 'Payee Type',
    amount: 'Amount',
    reference: 'Reference',
    description: 'Description',
  };

  readonly form: FormGroup;

  readonly defaultReference = `PAY-${Date.now()}`;

  // amountValue is a signal so the computed resultLabel re-evaluates on input changes.
  readonly amountValue = signal<number | null>(null);

  readonly resultLabel = computed<string>(() => {
    const amount = this.amountValue();
    if (amount === null || isNaN(amount) || amount <= 0) return '';
    return formatMoney(amount, this.data.wallet.currency);
  });

  constructor() {
    this.form = this.fb.group({
      payeeName: ['', [Validators.required]],
      payeeId: ['', [Validators.required]],
      payeeType: ['MERCHANT', [Validators.required]],
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
      payee: {
        name: v.payeeName,
        id: v.payeeId,
        type: v.payeeType,
      },
      amount: parseFloat(v.amount),
      reference: v.reference,
      description: v.description || undefined,
    } satisfies PaymentDialogResult);
  }

  onOverlayKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      this.cancel();
    }
  }
}
