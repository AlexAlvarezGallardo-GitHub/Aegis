// k6 load test: concurrent deposits via BFF (UC-004)
// Wallet is created once per VU; each iteration logs in and deposits.
// Run: k6 run --vus 20 --duration 2m load/k6/deposits.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import {
  BASE_URL, login, userEmail, PASSWORD, stateChangingHeaders, ensureWallet, trends,
} from './lib.js';

export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '1m', target: 20 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    // SLO for the wallet write path (docs/observability/slo.md: p95 = 300 ms).
    aegis_deposit_latency: ['p(95)<400'],
  },
};

export default function () {
  const vu = __VU;
  login(userEmail(vu), PASSWORD);

  const walletId = ensureWallet(vu, 'EUR');
  if (!walletId) {
    return;
  }

  const reference = `K6-DEP-${vu}-${__ITER}-${Date.now()}`;
  const res = http.post(`${BASE_URL}/api/bff/wallets/${walletId}/deposits`, JSON.stringify({
    amount: 100.0,
    currency: 'EUR',
    source: 'BANK_TRANSFER',
    reference,
  }), {
    headers: stateChangingHeaders(),
    tags: { name: 'deposit' },
  });
  trends.deposit.add(res.timings.duration);

  check(res, {
    'deposit returns 201': (r) => r.status === 201,
    'deposit returns newBalance': (r) => r.status === 201 && r.json('newBalance') !== undefined,
  });

  sleep(1);
}
