import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateWalletRequest, DepositFundsRequest, DepositReceipt, WalletResponse } from '../../shared/models/wallet.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class WalletService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/bff/wallets`;

  createWallet(request: CreateWalletRequest): Observable<WalletResponse> {
    return this.http.post<WalletResponse>(this.baseUrl, request);
  }

  getWallets(): Observable<WalletResponse[]> {
    return this.http.get<WalletResponse[]>(this.baseUrl);
  }

  getWallet(walletId: string): Observable<WalletResponse> {
    return this.http.get<WalletResponse>(`${this.baseUrl}/${walletId}`);
  }

  depositFunds(walletId: string, request: DepositFundsRequest): Observable<DepositReceipt> {
    return this.http.post<DepositReceipt>(`${this.baseUrl}/${walletId}/deposits`, request);
  }

  adjustBalance(walletId: string, amount: number, description?: string): Observable<WalletResponse> {
    return this.http.patch<WalletResponse>(`${this.baseUrl}/${walletId}/balance`, { amount, description });
  }

  updateStatus(walletId: string, status: string): Observable<WalletResponse> {
    return this.http.patch<WalletResponse>(`${this.baseUrl}/${walletId}/status`, { status });
  }
}
