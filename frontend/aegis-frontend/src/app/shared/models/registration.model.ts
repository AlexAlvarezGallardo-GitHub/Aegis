export interface RegisterUserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface RegisterUserResponse {
  userId: string;
  email: string;
  status: string;
  registeredAt: string;
}

export type { ErrorResponse } from './error.model';
