export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  emailVerified: boolean;
}

export interface ErrorResponse {
  code: string;
  message: string;
  details: Record<string, string> | null;
  timestamp: string;
}
