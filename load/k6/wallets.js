// k6 load test: create (once per VU) + list wallets via BFF (UC-003)
// Wallet creation is one-time onboarding; the common read path is listing.
// Each iteration uses a fresh authenticated session (login per iteration).
// Run: k6 run --vus 30 --duration 2m load/k6/wallets.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import {
  BASE_URL, login, userEmail, PASSWORD, ensureWallet, trends,
} from './lib.js';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 30 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    // SLO for the wallet read path (docs/observability/slo.md: p95 = 300 ms).
    aegis_list_wallets_latency: ['p(95)<300'],
    aegis_create_wallet_latency: ['p(95)<300'],
  },
};

export default function () {
  const vu = __VU;
  login(userEmail(vu), PASSWORD);

  const walletId = ensureWallet(vu, 'EUR');
  if (!walletId) {
    return;
  }

  const list = http.get(`${BASE_URL}/api/bff/wallets`, {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'list_wallets' },
  });
  trends.listWallets.add(list.timings.duration);
  check(list, {
    'list wallets returns 200': (r) => r.status === 200,
    'list returns the created wallet': (r) => r.status === 200 && r.body.includes(walletId),
  });

  sleep(1);
}
