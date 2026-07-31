import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, BehaviorSubject, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { LoginRequest, LoginResponse } from '../../shared/models/auth.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/bff/auth`;
  private authState = new BehaviorSubject<boolean>(false);

  readonly isAuthenticated$ = this.authState.asObservable();

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, request).pipe(
      tap(() => this.authState.next(true)),
    );
  }

  mockLogin(): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/mock-login`, {}).pipe(
      tap(() => this.authState.next(true)),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/logout`, {}).pipe(
      tap(() => this.authState.next(false)),
    );
  }

  getCurrentUser(): Observable<{ userId: string; email: string }> {
    return this.http.get<{ userId: string; email: string }>(`${this.baseUrl}/me`);
  }

  refresh(): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/refresh`, {}).pipe(
      tap(() => this.authState.next(true)),
    );
  }

  isAuthenticated(): boolean {
    return this.authState.value;
  }

  checkSession(): Observable<boolean> {
    return this.http.get<{ userId: string; email: string }>(`${this.baseUrl}/me`).pipe(
      tap(() => this.authState.next(true)),
      map(() => true),
      catchError(() => {
        this.authState.next(false);
        return of(false);
      }),
    );
  }

  setAuthenticated(): void {
    this.authState.next(true);
  }

  setUnauthenticated(): void {
    this.authState.next(false);
  }
}
