import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WalletService } from './wallet.service';
import { TransferRequest, TransferResponse, PaymentRequest, PaymentResponse } from '../../shared/models/wallet.model';

describe('WalletService', () => {
  let service: WalletService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [WalletService, provideHttpClient(withInterceptors([])), provideHttpClientTesting()],
    });

    service = TestBed.inject(WalletService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('transferFunds', () => {
    it('should POST to /api/bff/transfers with correct body', () => {
      const request: TransferRequest = {
        sourceWalletId: 'source-123',
        destWalletId: 'dest-456',
        amount: 100,
        currency: 'USD',
        reference: 'TRX-123',
        description: 'Test transfer',
      };

      const mockResponse: TransferResponse = {
        transferId: 'transfer-789',
        status: 'COMPLETED',
        sourceWalletId: 'source-123',
        destWalletId: 'dest-456',
        userId: 'user-001',
        amount: 100,
        currency: 'USD',
        reference: 'TRX-123',
        description: 'Test transfer',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      };

      service.transferFunds(request).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne('/api/bff/transfers');
      expect(req.request.method).toBe('POST');
      expect(req.request.url).toBe('/api/bff/transfers');
      expect(req.request.url).not.toContain('localhost');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });

    it('should handle transfer without description', () => {
      const request: TransferRequest = {
        sourceWalletId: 'source-123',
        destWalletId: 'dest-456',
        amount: 50,
        currency: 'EUR',
        reference: 'TRX-456',
      };

      service.transferFunds(request).subscribe();

      const req = httpMock.expectOne('/api/bff/transfers');
      expect(req.request.body).toEqual(request);
      req.flush({
        transferId: 'transfer-999',
        status: 'COMPLETED',
        sourceWalletId: 'source-123',
        destWalletId: 'dest-456',
        userId: 'user-001',
        amount: 50,
        currency: 'EUR',
        reference: 'TRX-456',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      });
    });
  });

  describe('getTransfer', () => {
    it('should GET from /api/bff/transfers/{transferId}', () => {
      const transferId = 'transfer-789';
      const mockResponse: TransferResponse = {
        transferId: 'transfer-789',
        status: 'COMPLETED',
        sourceWalletId: 'source-123',
        destWalletId: 'dest-456',
        userId: 'user-001',
        amount: 100,
        currency: 'USD',
        reference: 'TRX-123',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
        completedAt: '2026-01-01T00:01:00Z',
      };

      service.getTransfer(transferId).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`/api/bff/transfers/${transferId}`);
      expect(req.request.method).toBe('GET');
      expect(req.request.url).toBe(`/api/bff/transfers/${transferId}`);
      expect(req.request.url).not.toContain('localhost');
      req.flush(mockResponse);
    });
  });

  describe('recordActivity', () => {
    it('should add activity to activities signal', () => {
      const activity = {
        walletId: 'wallet-123',
        type: 'TRANSFER' as const,
        amount: -100,
        currency: 'USD',
        reference: 'TRX-123',
        status: 'COMPLETED' as const,
        timestamp: '2026-01-01T00:00:00Z',
      };

      service.recordActivity(activity);

      const activities = service.getActivitiesFor('wallet-123');
      expect(activities.length).toBe(1);
      expect(activities[0]).toEqual(jasmine.objectContaining(activity));
    });
  });

  describe('executePayment', () => {
    it('should POST to /api/bff/payments with correct body', () => {
      const request: PaymentRequest = {
        walletId: 'wallet-123',
        amount: 50,
        currency: 'USD',
        payee: { name: 'Acme Corp', id: 'acme-001', type: 'MERCHANT' },
        reference: 'PAY-123',
        description: 'Test payment',
      };

      const mockResponse: PaymentResponse = {
        paymentId: 'payment-789',
        status: 'COMPLETED',
        walletId: 'wallet-123',
        userId: 'user-001',
        amount: 50,
        currency: 'USD',
        payee: { name: 'Acme Corp', id: 'acme-001', type: 'MERCHANT' },
        reference: 'PAY-123',
        description: 'Test payment',
        createdAt: '2026-01-01T00:00:00Z',
      };

      service.executePayment(request).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne('/api/bff/payments');
      expect(req.request.method).toBe('POST');
      expect(req.request.url).toBe('/api/bff/payments');
      expect(req.request.url).not.toContain('localhost');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });

    it('should handle payment without description', () => {
      const request: PaymentRequest = {
        walletId: 'wallet-123',
        amount: 25,
        currency: 'EUR',
        payee: { name: 'Store', id: 'store-001', type: 'INDIVIDUAL' },
        reference: 'PAY-456',
      };

      service.executePayment(request).subscribe();

      const req = httpMock.expectOne('/api/bff/payments');
      expect(req.request.body).toEqual(request);
      req.flush({
        paymentId: 'payment-999',
        status: 'COMPLETED',
        walletId: 'wallet-123',
        userId: 'user-001',
        amount: 25,
        currency: 'EUR',
        payee: { name: 'Store', id: 'store-001', type: 'INDIVIDUAL' },
        reference: 'PAY-456',
        createdAt: '2026-01-01T00:00:00Z',
      });
    });
  });

  describe('getPayment', () => {
    it('should GET from /api/bff/payments/{paymentId}', () => {
      const paymentId = 'payment-789';
      const mockResponse: PaymentResponse = {
        paymentId: 'payment-789',
        status: 'COMPLETED',
        walletId: 'wallet-123',
        userId: 'user-001',
        amount: 50,
        currency: 'USD',
        payee: { name: 'Acme Corp', id: 'acme-001', type: 'MERCHANT' },
        reference: 'PAY-123',
        createdAt: '2026-01-01T00:00:00Z',
        completedAt: '2026-01-01T00:01:00Z',
      };

      service.getPayment(paymentId).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`/api/bff/payments/${paymentId}`);
      expect(req.request.method).toBe('GET');
      expect(req.request.url).toBe(`/api/bff/payments/${paymentId}`);
      expect(req.request.url).not.toContain('localhost');
      req.flush(mockResponse);
    });
  });
});
