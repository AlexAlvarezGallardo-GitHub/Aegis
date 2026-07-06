export interface ErrorResponse {
  code: string;
  message: string;
  details: Record<string, string> | null;
  timestamp: string;
}
