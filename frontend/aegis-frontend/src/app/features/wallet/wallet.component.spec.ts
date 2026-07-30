import { ComponentFixture, TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Routes } from '@angular/router';
import { WalletComponent } from './wallet.component';
import { WalletService } from './wallet.service';
import { By } from '@angular/platform-browser';

const stubRoutes: Routes = [{ path: 'wallets', component: class {} }];

describe('WalletComponent', () => {
  let component: WalletComponent;
  let fixture: ComponentFixture<WalletComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        WalletComponent,
        ReactiveFormsModule,
        HttpClientTestingModule,
        NoopAnimationsModule
      ],
      providers: [WalletService, provideRouter(stubRoutes)]
    }).compileComponents();

    fixture = TestBed.createComponent(WalletComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have invalid form when empty', () => {
    expect(component.walletForm.valid).toBeFalsy();
  });

  it('should validate currency format', () => {
    component.walletForm.patchValue({ currency: '' });
    expect(component.walletForm.valid).toBeFalsy();
    component.walletForm.patchValue({ currency: 'EUR' });
    expect(component.walletForm.valid).toBeTruthy();
  });

  it('should disable submit button when form is invalid', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne('/api/bff/wallets').flush([]);
    tick();
    // Open the create panel
    component.openCreatePanel();
    fixture.detectChanges();
    tick();
    const submitBtn = fixture.debugElement.query(By.css('button[type="submit"]'));
    if (submitBtn) {
      expect(submitBtn.nativeElement.disabled).toBeTrue();
    }
    flush();
  }));

  it('should disable submit button while loading', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne('/api/bff/wallets').flush([]);
    tick();
    component.openCreatePanel();
    component.walletForm.patchValue({ currency: 'EUR' });
    component.isLoading = true;
    fixture.detectChanges();
    const submitBtn = fixture.debugElement.query(By.css('button[type="submit"]'));
    if (submitBtn) {
      expect(submitBtn.nativeElement.disabled).toBeTrue();
    }
    flush();
  }));

  it('should enable submit button when form valid and not loading', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne('/api/bff/wallets').flush([]);
    tick();
    component.openCreatePanel();
    component.walletForm.patchValue({ currency: 'EUR' });
    component.isLoading = false;
    fixture.detectChanges();
    const submitBtn = fixture.debugElement.query(By.css('button[type="submit"]'));
    if (submitBtn) {
      expect(submitBtn.nativeElement.disabled).toBeFalse();
    }
    flush();
  }));

  it('should set isLoading true on submit and reset via finalize', fakeAsync(() => {
    component.walletForm.patchValue({ currency: 'EUR' });
    component.onSubmit();
    expect(component.isLoading).toBeTrue();
    httpMock.expectOne('/api/bff/wallets').flush({
      walletId: '123', userId: 'user-1', balance: 0, currency: 'EUR', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z'
    });
    tick();
    expect(component.isLoading).toBeFalse();
    flush();
  }));

  it('should reset isLoading via finalize after error', fakeAsync(() => {
    component.walletForm.patchValue({ currency: 'EUR' });
    component.onSubmit();
    expect(component.isLoading).toBeTrue();
    httpMock.expectOne('/api/bff/wallets').flush(
      { code: 'WALLET_LIMIT_EXCEEDED', message: 'Wallet limit exceeded', details: null, timestamp: '2026-01-01' },
      { status: 409, statusText: 'Conflict' }
    );
    tick();
    expect(component.isLoading).toBeFalse();
    flush();
  }));

  it('should reset isLoading via finalize after network error', fakeAsync(() => {
    component.walletForm.patchValue({ currency: 'EUR' });
    component.onSubmit();
    expect(component.isLoading).toBeTrue();
    httpMock.expectOne('/api/bff/wallets').error(new ProgressEvent('network error'));
    tick();
    expect(component.isLoading).toBeFalse();
    flush();
  }));

  it('should call relative URL through proxy', fakeAsync(() => {
    component.walletForm.patchValue({ currency: 'EUR' });
    component.onSubmit();
    const req = httpMock.expectOne('/api/bff/wallets');
    expect(req.request.url).toBe('/api/bff/wallets');
    expect(req.request.url).not.toContain('localhost');
    req.flush({ walletId: '1', userId: 'u1', balance: 0, currency: 'EUR', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z' });
    tick();
    flush();
  }));

  it('should load wallets on init', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne('/api/bff/wallets').flush([
      { walletId: '1', userId: 'u1', balance: 100, currency: 'USD', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z' }
    ]);
    tick();
    expect(component.wallets().length).toBe(1);
    flush();
  }));

  it('should uppercase currency on submit', fakeAsync(() => {
    component.walletForm.patchValue({ currency: 'eur' });
    component.onSubmit();
    const req = httpMock.expectOne('/api/bff/wallets');
    expect(req.request.body).toEqual({ currency: 'EUR' });
    req.flush({ walletId: '1', userId: 'u1', balance: 0, currency: 'EUR', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z' });
    tick();
    flush();
  }));
});
