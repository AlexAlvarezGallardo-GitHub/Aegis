import { ComponentFixture, TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Routes } from '@angular/router';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Dialog, DialogRef } from '@angular/cdk/dialog';
import { WalletDetailComponent } from './wallet-detail.component';
import { WalletService } from '../wallet.service';
import { ToastService } from '../../../shared/services/toast.service';
import { TransferDialogComponent, TransferDialogResult } from './transfer-dialog.component';
import { PaymentDialogComponent, PaymentDialogResult } from './payment-dialog.component';

const stubRoutes: Routes = [{ path: 'wallets', component: class {} }];

describe('WalletDetailComponent', () => {
  let component: WalletDetailComponent;
  let fixture: ComponentFixture<WalletDetailComponent>;
  let httpMock: HttpTestingController;
  let walletService: jasmine.SpyObj<WalletService>;
  let toastService: jasmine.SpyObj<ToastService>;
  let dialog: jasmine.SpyObj<Dialog>;

  const mockWallet = {
    walletId: 'wallet-123',
    userId: 'user-456',
    balance: 1000,
    currency: 'USD',
    status: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    const walletServiceSpy = jasmine.createSpyObj('WalletService', [
      'getWallet',
      'transferFunds',
      'executePayment',
      'recordActivity',
      'getActivitiesFor',
    ]);
    const toastServiceSpy = jasmine.createSpyObj('ToastService', ['success', 'error', 'warning']);
    const dialogSpy = jasmine.createSpyObj('Dialog', ['open']);

    await TestBed.configureTestingModule({
      imports: [WalletDetailComponent, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        provideRouter(stubRoutes),
        provideHttpClient(withInterceptors([])),
        provideHttpClientTesting(),
        { provide: WalletService, useValue: walletServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: Dialog, useValue: dialogSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ walletId: 'wallet-123' })),
            queryParams: of({}),
          },
        },
      ],
    }).compileComponents();

    walletService = TestBed.inject(WalletService) as jasmine.SpyObj<WalletService>;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    dialog = TestBed.inject(Dialog) as jasmine.SpyObj<Dialog>;

    // Stub the service BEFORE createComponent: the constructor subscribes to
    // queryParams and calls loadWallet() synchronously, so getWallet must be ready.
    walletService.getWallet.and.returnValue(of(mockWallet));
    walletService.getActivitiesFor.and.returnValue([]);

    fixture = TestBed.createComponent(WalletDetailComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load wallet on init', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    expect(component.wallet()).toEqual(mockWallet);
    flush();
  }));

  describe('Transfer functionality', () => {
    it('should open transfer dialog when openTransfer is called', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      // Closed without a result — openTransfer must not attempt the transfer.
      const mockDialogRef = {
        closed: of(undefined),
      };
      dialog.open.and.returnValue(mockDialogRef as unknown as DialogRef<unknown, unknown>);

      component.openTransfer();

      expect(dialog.open).toHaveBeenCalledWith(TransferDialogComponent, {
        data: { wallet: mockWallet },
      });
      flush();
    }));

    it('should call transferFunds with correct parameters on successful transfer', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      const mockDialogRef = {
        closed: of({ destWalletId: 'dest-wallet-123', amount: 100, reference: 'TRX-123' } as TransferDialogResult),
      };
      dialog.open.and.returnValue(mockDialogRef as unknown as DialogRef<unknown, unknown>);

      walletService.transferFunds.and.returnValue(of({
        transferId: 'transfer-123',
        status: 'COMPLETED',
        sourceWalletId: 'wallet-123',
        destWalletId: 'dest-wallet-123',
        userId: 'user-456',
        amount: 100,
        currency: 'USD',
        reference: 'TRX-123',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      }));

      component.openTransfer();
      tick();

      expect(walletService.transferFunds).toHaveBeenCalledWith({
        sourceWalletId: 'wallet-123',
        destWalletId: 'dest-wallet-123',
        amount: 100,
        currency: 'USD',
        reference: 'TRX-123',
        description: undefined,
      });

      expect(walletService.recordActivity).toHaveBeenCalledWith(jasmine.objectContaining({
        walletId: 'wallet-123',
        type: 'TRANSFER',
        amount: -100,
        currency: 'USD',
        reference: 'TRX-123',
        status: 'COMPLETED',
      }));

      expect(toastService.success).toHaveBeenCalledWith('Transfer completed', jasmine.any(Object));
      flush();
    }));

    it('should reset isOperating via finalize on error', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      const mockDialogRef = {
        closed: of({ destWalletId: 'dest-wallet-123', amount: 100, reference: 'TRX-123' } as TransferDialogResult),
      };
      dialog.open.and.returnValue(mockDialogRef as unknown as DialogRef<unknown, unknown>);

      walletService.transferFunds.and.returnValue(throwError(() => ({ status: 500 })));

      component.openTransfer();
      tick();

      expect(component.isOperating()).toBeFalse();
      flush();
    }));

    it('should show warning toast and record REJECTED activity on 409 error', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      const mockDialogRef = {
        closed: of({ destWalletId: 'dest-wallet-123', amount: 100, reference: 'TRX-123' } as TransferDialogResult),
      };
      dialog.open.and.returnValue(mockDialogRef as unknown as DialogRef<unknown, unknown>);

      walletService.transferFunds.and.returnValue(throwError(() => ({ status: 409 })));

      component.openTransfer();
      tick();

      expect(walletService.recordActivity).toHaveBeenCalledWith(jasmine.objectContaining({
        walletId: 'wallet-123',
        type: 'TRANSFER',
        status: 'REJECTED',
      }));

      expect(toastService.warning).toHaveBeenCalledWith('Duplicate transfer reference.');
      flush();
    }));

    it('should show error toast with message on 422 error', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      const mockDialogRef = {
        closed: of({ destWalletId: 'dest-wallet-123', amount: 100, reference: 'TRX-123' } as TransferDialogResult),
      };
      dialog.open.and.returnValue(mockDialogRef as unknown as DialogRef<unknown, unknown>);

      walletService.transferFunds.and.returnValue(throwError(() => ({
        status: 422,
        error: { message: 'Insufficient funds' },
      })));

      component.openTransfer();
      tick();

      expect(toastService.error).toHaveBeenCalledWith('Unable to complete transfer', {
        description: 'Insufficient funds',
      });
      flush();
    }));

    it('should show generic error toast on other errors', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      const mockDialogRef = {
        closed: of({ destWalletId: 'dest-wallet-123', amount: 100, reference: 'TRX-123' } as TransferDialogResult),
      };
      dialog.open.and.returnValue(mockDialogRef as unknown as DialogRef<unknown, unknown>);

      walletService.transferFunds.and.returnValue(throwError(() => ({ status: 500 })));

      component.openTransfer();
      tick();

      expect(toastService.error).toHaveBeenCalledWith('Unable to complete transfer', {
        description: 'Please try again.',
      });
      flush();
    }));
  });

  describe('Activity type labels and icons', () => {
    it('should return correct label for TRANSFER', () => {
      expect(component.activityTypeLabel('TRANSFER')).toBe('Transfer');
    });

    it('should return correct icon for TRANSFER', () => {
      expect(component.activityIcon('TRANSFER')).toBe('swap_horiz');
    });

    it('should return correct label for PAYMENT', () => {
      expect(component.activityTypeLabel('PAYMENT')).toBe('Payment');
    });

    it('should return correct icon for PAYMENT', () => {
      expect(component.activityIcon('PAYMENT')).toBe('payments');
    });
  });

  describe('Payment functionality', () => {
    it('should open payment dialog when openPayment is called', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      const mockDialogRef = {
        closed: of(undefined),
      };
      dialog.open.and.returnValue(mockDialogRef as unknown as DialogRef<unknown, unknown>);

      component.openPayment();

      expect(dialog.open).toHaveBeenCalledWith(PaymentDialogComponent, {
        data: { wallet: mockWallet },
      });
      flush();
    }));

    it('should call executePayment with correct parameters on successful payment', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      const paymentResult: PaymentDialogResult = {
        payee: { name: 'Acme Corp', id: 'acme-001', type: 'MERCHANT' },
        amount: 50,
        reference: 'PAY-123',
        description: 'Test payment',
      };
      const mockDialogRef = {
        closed: of(paymentResult),
      };
      dialog.open.and.returnValue(mockDialogRef as unknown as DialogRef<unknown, unknown>);

      walletService.executePayment.and.returnValue(of({
        paymentId: 'payment-123',
        status: 'COMPLETED',
        walletId: 'wallet-123',
        userId: 'user-456',
        amount: 50,
        currency: 'USD',
        payee: { name: 'Acme Corp', id: 'acme-001', type: 'MERCHANT' },
        reference: 'PAY-123',
        createdAt: '2026-01-01T00:00:00Z',
      }));

      component.openPayment();
      tick();

      expect(walletService.executePayment).toHaveBeenCalledWith({
        walletId: 'wallet-123',
        amount: 50,
        currency: 'USD',
        payee: { name: 'Acme Corp', id: 'acme-001', type: 'MERCHANT' },
        reference: 'PAY-123',
        description: 'Test payment',
      });

      expect(walletService.recordActivity).toHaveBeenCalledWith(jasmine.objectContaining({
        walletId: 'wallet-123',
        type: 'PAYMENT',
        amount: -50,
        currency: 'USD',
        reference: 'PAY-123',
        status: 'COMPLETED',
      }));

      expect(toastService.success).toHaveBeenCalledWith('Payment completed', jasmine.any(Object));
      flush();
    }));

    it('should reset isOperating via finalize on error', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      const paymentResult: PaymentDialogResult = {
        payee: { name: 'Acme Corp', id: 'acme-001', type: 'MERCHANT' },
        amount: 50,
        reference: 'PAY-123',
      };
      const mockDialogRef = {
        closed: of(paymentResult),
      };
      dialog.open.and.returnValue(mockDialogRef as unknown as DialogRef<unknown, unknown>);

      walletService.executePayment.and.returnValue(throwError(() => ({ status: 500 })));

      component.openPayment();
      tick();

      expect(component.isOperating()).toBeFalse();
      flush();
    }));

    it('should show warning toast and record REJECTED activity on 409 error', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      const paymentResult: PaymentDialogResult = {
        payee: { name: 'Acme Corp', id: 'acme-001', type: 'MERCHANT' },
        amount: 50,
        reference: 'PAY-123',
      };
      const mockDialogRef = {
        closed: of(paymentResult),
      };
      dialog.open.and.returnValue(mockDialogRef as unknown as DialogRef<unknown, unknown>);

      walletService.executePayment.and.returnValue(throwError(() => ({ status: 409 })));

      component.openPayment();
      tick();

      expect(walletService.recordActivity).toHaveBeenCalledWith(jasmine.objectContaining({
        walletId: 'wallet-123',
        type: 'PAYMENT',
        status: 'REJECTED',
      }));

      expect(toastService.warning).toHaveBeenCalledWith('Duplicate payment reference.');
      flush();
    }));

    it('should show error toast with message on 422 error', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      const paymentResult: PaymentDialogResult = {
        payee: { name: 'Acme Corp', id: 'acme-001', type: 'MERCHANT' },
        amount: 50,
        reference: 'PAY-123',
      };
      const mockDialogRef = {
        closed: of(paymentResult),
      };
      dialog.open.and.returnValue(mockDialogRef as unknown as DialogRef<unknown, unknown>);

      walletService.executePayment.and.returnValue(throwError(() => ({
        status: 422,
        error: { message: 'Insufficient funds' },
      })));

      component.openPayment();
      tick();

      expect(toastService.error).toHaveBeenCalledWith('Unable to complete payment', {
        description: 'Insufficient funds',
      });
      flush();
    }));

    it('should show generic error toast on other errors', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      const paymentResult: PaymentDialogResult = {
        payee: { name: 'Acme Corp', id: 'acme-001', type: 'MERCHANT' },
        amount: 50,
        reference: 'PAY-123',
      };
      const mockDialogRef = {
        closed: of(paymentResult),
      };
      dialog.open.and.returnValue(mockDialogRef as unknown as DialogRef<unknown, unknown>);

      walletService.executePayment.and.returnValue(throwError(() => ({ status: 500 })));

      component.openPayment();
      tick();

      expect(toastService.error).toHaveBeenCalledWith('Unable to complete payment', {
        description: 'Please try again.',
      });
      flush();
    }));
  });
});
