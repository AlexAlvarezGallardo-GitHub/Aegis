import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RegisterUserRequest, RegisterUserResponse } from '../../shared/models/registration.model';

/**
 * Registration service.
 *
 * NOTE: This service calls the Identity microservice directly at `/api/v1/users/register`
 * instead of going through the BFF (`/api/bff/...`). This is intentional: registration is a
 * public, pre-authentication endpoint and the BFF currently only exposes authenticated
 * endpoints (login, refresh, logout, me). When the BFF adds a register endpoint, this URL
 * should be updated to `/api/bff/auth/register`.
 */
@Injectable({
  providedIn: 'root'
})
export class RegistrationService {
  private http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/users';

  register(request: RegisterUserRequest): Observable<RegisterUserResponse> {
    return this.http.post<RegisterUserResponse>(`${this.baseUrl}/register`, request);
  }
}
