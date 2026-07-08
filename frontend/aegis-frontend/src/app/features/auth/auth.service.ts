import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse } from '../../shared/models/auth.model';
import { environment } from '../../../environments/environment';

const TOKEN_KEY = 'aegis_access_token';
const REFRESH_KEY = 'aegis_refresh_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/bff/auth`;

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, request).pipe(
      tap((response) => this.storeTokens(response.accessToken, response.refreshToken)),
    );
  }

  mockLogin(): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/mock-login`, {}).pipe(
      tap((response) => this.storeTokens(response.accessToken, response.refreshToken)),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/logout`, {}).pipe(
      tap(() => this.clearTokens()),
    );
  }

  getCurrentUser(): Observable<{ userId: string; email: string }> {
    return this.http.get<{ userId: string; email: string }>(`${this.baseUrl}/me`);
  }

  refresh(): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/refresh`, {}).pipe(
      tap((response) => this.storeTokens(response.accessToken, response.refreshToken)),
    );
  }

  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }

  getAccessToken(): string | null {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem(TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem(REFRESH_KEY);
  }

  private storeTokens(accessToken: string, refreshToken: string): void {
    if (typeof localStorage === 'undefined') return;
    localStorage.setItem(TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_KEY, refreshToken);
  }

  clearTokens(): void {
    if (typeof localStorage === 'undefined') return;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
  }
}
