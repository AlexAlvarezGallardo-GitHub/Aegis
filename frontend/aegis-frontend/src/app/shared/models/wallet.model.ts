export interface CreateWalletRequest {
  currency: string;
}

export interface WalletResponse {
  walletId: string;
  userId: string;
  balance: number;
  currency: string;
  status: string;
  premium?: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface WalletListResponse {
  wallets: WalletResponse[];
}

export interface DepositFundsRequest {
  amount: number;
  currency: string;
  source: string;
  reference: string;
}

export interface DepositReceipt {
  depositId: string;
  walletId: string;
  newBalance: number;
  amount: number;
  currency: string;
  source: string;
  reference: string;
  timestamp: string;
}

export type WalletActivityType = 'DEPOSIT' | 'WITHDRAWAL' | 'ADJUSTMENT' | 'TRANSFER' | 'PAYMENT';

export interface TransferRequest {
  sourceWalletId: string;
  destWalletId: string;
  amount: number;
  currency: string;
  description?: string;
  reference: string;
}

export interface TransferResponse {
  transferId: string;
  status: string;
  sourceWalletId: string;
  destWalletId: string;
  userId: string;
  amount: number;
  currency: string;
  description?: string | null;
  reference: string;
  fraudAssessmentId?: string | null;
  holdId?: string | null;
  failureReason?: string | null;
  createdAt: string;
  updatedAt: string;
  completedAt?: string | null;
}

export interface Payee {
  name: string;
  id: string;
  type: 'MERCHANT' | 'INDIVIDUAL' | 'SERVICE';
}

export interface PaymentRequest {
  walletId: string;
  amount: number;
  currency: string;
  payee: Payee;
  description?: string;
  reference: string;
}

export interface PaymentResponse {
  paymentId: string;
  status: string;
  walletId: string;
  userId: string;
  amount: number;
  currency: string;
  payee: Payee;
  description?: string | null;
  reference: string;
  fraudAssessmentId?: string | null;
  holdId?: string | null;
  failureReason?: string | null;
  createdAt: string;
  updatedAt?: string;
  completedAt?: string | null;
}

export interface WalletActivity {
  id: string;
  walletId: string;
  type: WalletActivityType;
  amount: number;
  currency: string;
  source?: string;
  reference?: string;
  status: 'COMPLETED' | 'REJECTED';
  timestamp: string;
}
