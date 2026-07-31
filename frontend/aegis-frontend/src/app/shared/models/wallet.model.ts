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
