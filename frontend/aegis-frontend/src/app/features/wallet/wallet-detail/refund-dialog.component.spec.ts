import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { RefundDialogComponent, RefundDialogData, RefundDialogResult } from './refund-dialog.component';

describe('RefundDialogComponent', () => {
  let component: RefundDialogComponent;
  let fixture: ComponentFixture<RefundDialogComponent>;
  let dialogRefSpy: jasmine.SpyObj<DialogRef<RefundDialogResult>>;

  const mockWallet = {
    walletId: 'wallet-123',
    userId: 'user-456',
    balance: 1000,
    currency: 'USD',
    status: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00Z',
  };

  const mockData: RefundDialogData = {
    wallet: mockWallet,
    paymentId: 'payment-123',
    paymentAmount: 50,
  };

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('DialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [RefundDialogComponent, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        { provide: DialogRef, useValue: dialogRefSpy },
        { provide: DIALOG_DATA, useValue: mockData },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RefundDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with default reference', () => {
    expect(component.form.get('reference')?.value).toContain('REF-');
  });

  it('should have invalid form when reference is empty', () => {
    // Clear the reference to make form invalid
    component.form.get('reference')?.setValue('');
    fixture.detectChanges();

    expect(component.form.invalid).toBeTrue();
  });

  it('should have valid form when reference is provided', () => {
    component.form.get('reference')?.setValue('REF-123');
    fixture.detectChanges();

    expect(component.form.valid).toBeTrue();
  });

  it('should close dialog with result on submit', () => {
    component.form.get('amount')?.setValue('25.50');
    component.form.get('reference')?.setValue('REF-123');
    component.form.get('reason')?.setValue('Product returned');

    component.submit();

    expect(dialogRefSpy.close).toHaveBeenCalledWith({
      amount: 25.50,
      reason: 'Product returned',
      reference: 'REF-123',
    });
  });

  it('should close dialog with undefined amount when not provided', () => {
    component.form.get('reference')?.setValue('REF-456');

    component.submit();

    expect(dialogRefSpy.close).toHaveBeenCalledWith({
      amount: undefined,
      reason: undefined,
      reference: 'REF-456',
    });
  });

  it('should close dialog without result on cancel', () => {
    component.cancel();

    expect(dialogRefSpy.close).toHaveBeenCalledWith();
  });

  it('should update resultLabel when amount changes', () => {
    component.form.get('amount')?.setValue('25.50');
    fixture.detectChanges();

    expect(component.resultLabel()).toContain('25.50');
  });
});
