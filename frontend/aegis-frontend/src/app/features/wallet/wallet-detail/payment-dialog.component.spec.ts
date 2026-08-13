import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { PaymentDialogComponent, PaymentDialogData, PaymentDialogResult } from './payment-dialog.component';

describe('PaymentDialogComponent', () => {
  let component: PaymentDialogComponent;
  let fixture: ComponentFixture<PaymentDialogComponent>;
  let dialogRefSpy: jasmine.SpyObj<DialogRef<PaymentDialogResult>>;

  const mockWallet = {
    walletId: 'wallet-123',
    userId: 'user-456',
    balance: 1000,
    currency: 'USD',
    status: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00Z',
  };

  const mockData: PaymentDialogData = {
    wallet: mockWallet,
  };

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('DialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [PaymentDialogComponent, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        { provide: DialogRef, useValue: dialogRefSpy },
        { provide: DIALOG_DATA, useValue: mockData },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have invalid form when empty', () => {
    component.form.patchValue({ payeeName: '', payeeId: '', amount: '', reference: '' });
    expect(component.form.valid).toBeFalsy();
  });

  it('should disable submit button when form is invalid', () => {
    component.form.patchValue({ payeeName: '', payeeId: '', amount: '', reference: '' });
    fixture.detectChanges();
    const submitBtn = fixture.nativeElement.querySelector('app-aegis-loading-button button');
    expect(submitBtn).toBeTruthy();
    expect(submitBtn.disabled).toBeTrue();
  });

  it('should disable submit button when payeeName is empty', () => {
    component.form.patchValue({ payeeName: '', payeeId: 'payee-1', amount: 100, reference: 'PAY-123' });
    fixture.detectChanges();
    expect(component.form.valid).toBeFalsy();
  });

  it('should disable submit button when payeeId is empty', () => {
    component.form.patchValue({ payeeName: 'Acme', payeeId: '', amount: 100, reference: 'PAY-123' });
    fixture.detectChanges();
    expect(component.form.valid).toBeFalsy();
  });

  it('should disable submit button when amount is <= 0', () => {
    component.form.patchValue({ payeeName: 'Acme', payeeId: 'payee-1', amount: 0, reference: 'PAY-123' });
    fixture.detectChanges();
    expect(component.form.valid).toBeFalsy();
  });

  it('should enable submit button when form is valid', () => {
    component.form.patchValue({
      payeeName: 'Acme Corp',
      payeeId: 'acme-001',
      payeeType: 'MERCHANT',
      amount: 100,
      reference: 'PAY-123',
    });
    fixture.detectChanges();
    expect(component.form.valid).toBeTruthy();
  });

  it('should not close dialog when submitting invalid form', () => {
    component.form.patchValue({ payeeName: '', payeeId: '', amount: '', reference: '' });
    component.submit();
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
  });

  it('should close dialog with result when submitting valid form', () => {
    component.form.patchValue({
      payeeName: 'Acme Corp',
      payeeId: 'acme-001',
      payeeType: 'MERCHANT',
      amount: 50.25,
      reference: 'PAY-123',
      description: 'Test payment',
    });
    component.submit();
    expect(dialogRefSpy.close).toHaveBeenCalledWith({
      payee: { name: 'Acme Corp', id: 'acme-001', type: 'MERCHANT' },
      amount: 50.25,
      reference: 'PAY-123',
      description: 'Test payment',
    });
  });

  it('should close dialog without result when cancel is called', () => {
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith();
  });

  it('should have default reference with timestamp', () => {
    expect(component.defaultReference).toMatch(/^PAY-\d+$/);
  });

  it('should compute resultLabel when amount is valid', () => {
    component.form.patchValue({ amount: 100 });
    fixture.detectChanges();
    expect(component.resultLabel()).toBe('$100.00');
  });

  it('should return empty resultLabel when amount is invalid', () => {
    component.form.patchValue({ amount: 0 });
    fixture.detectChanges();
    expect(component.resultLabel()).toBe('');
  });

  it('should default payeeType to MERCHANT', () => {
    expect(component.form.get('payeeType')?.value).toBe('MERCHANT');
  });
});
