export interface CreateWalletRequest {
  currency: string;
}

export interface WalletResponse {
  walletId: string;
  userId: string;
  balance: number;
  currency: string;
  status: string;
  createdAt: string;
}

export interface WalletListResponse {
  wallets: WalletResponse[];
}
