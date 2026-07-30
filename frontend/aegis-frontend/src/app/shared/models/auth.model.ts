export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  tokenType: string;
  expiresIn: number;
  emailVerified: boolean;
}

export type { ErrorResponse } from './error.model';
