import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { TransferDialogComponent, TransferDialogData, TransferDialogResult } from './transfer-dialog.component';

describe('TransferDialogComponent', () => {
  let component: TransferDialogComponent;
  let fixture: ComponentFixture<TransferDialogComponent>;
  let dialogRefSpy: jasmine.SpyObj<DialogRef<TransferDialogResult>>;

  const mockWallet = {
    walletId: 'wallet-123',
    userId: 'user-456',
    balance: 1000,
    currency: 'USD',
    status: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00Z',
  };

  const mockData: TransferDialogData = {
    wallet: mockWallet,
  };

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('DialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [TransferDialogComponent, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        { provide: DialogRef, useValue: dialogRefSpy },
        { provide: DIALOG_DATA, useValue: mockData },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TransferDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have invalid form when empty', () => {
    component.form.patchValue({ destWalletId: '', amount: '', reference: '' });
    expect(component.form.valid).toBeFalsy();
  });

  it('should disable submit button when form is invalid', () => {
    component.form.patchValue({ destWalletId: '', amount: '', reference: '' });
    fixture.detectChanges();
    const submitBtn = fixture.nativeElement.querySelector('app-aegis-loading-button button');
    expect(submitBtn).toBeTruthy();
    expect(submitBtn.disabled).toBeTrue();
  });

  it('should disable submit button when destWalletId is too short', () => {
    component.form.patchValue({ destWalletId: 'short', amount: 100, reference: 'TRX-123' });
    fixture.detectChanges();
    expect(component.form.valid).toBeFalsy();
  });

  it('should disable submit button when amount is <= 0', () => {
    component.form.patchValue({ destWalletId: 'wallet-abc-123', amount: 0, reference: 'TRX-123' });
    fixture.detectChanges();
    expect(component.form.valid).toBeFalsy();
  });

  it('should enable submit button when form is valid', () => {
    component.form.patchValue({ destWalletId: 'wallet-abc-123', amount: 100, reference: 'TRX-123' });
    fixture.detectChanges();
    expect(component.form.valid).toBeTruthy();
  });

  it('should not close dialog when submitting invalid form', () => {
    component.form.patchValue({ destWalletId: '', amount: '', reference: '' });
    component.submit();
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
  });

  it('should close dialog with result when submitting valid form', () => {
    component.form.patchValue({
      destWalletId: 'wallet-abc-123',
      amount: 100.50,
      reference: 'TRX-123',
      description: 'Test transfer',
    });
    component.submit();
    expect(dialogRefSpy.close).toHaveBeenCalledWith({
      destWalletId: 'wallet-abc-123',
      amount: 100.50,
      reference: 'TRX-123',
      description: 'Test transfer',
    });
  });

  it('should close dialog without result when cancel is called', () => {
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith();
  });

  it('should have default reference with timestamp', () => {
    expect(component.defaultReference).toMatch(/^TRX-\d+$/);
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
});
