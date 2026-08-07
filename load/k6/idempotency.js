// k6 load test: idempotency - repeat the same deposit reference
// Expected: exactly one 201, subsequent attempts return 409 DuplicateDeposit.
// Run: k6 run --vus 10 --duration 1m load/k6/idempotency.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '1m',
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const FIXED_REFERENCE = 'IDEMPOTENCY-FIXED-REF';

const wallets = {};

function ensureWallet(userId) {
  if (wallets[userId]) {
    return wallets[userId];
  }
  const create = http.post(`${BASE_URL}/api/bff/wallets`, JSON.stringify({
    currency: 'EUR',
  }), {
    headers: { 'Content-Type': 'application/json', 'X-User-Id': userId },
  });
  if (create.status === 201) {
    wallets[userId] = create.json('walletId');
  }
  return wallets[userId];
}

export default function () {
  const userId = `idem-user-${__VU}`;
  const walletId = ensureWallet(userId);
  if (!walletId) {
    return;
  }

  // Reuse the SAME reference every iteration for this VU
  const res = http.post(`${BASE_URL}/api/bff/wallets/${walletId}/deposits`, JSON.stringify({
    amount: 50.0,
    currency: 'EUR',
    source: 'BANK_TRANSFER',
    reference: `${FIXED_REFERENCE}-${__VU}`,
  }), {
    headers: { 'Content-Type': 'application/json', 'X-User-Id': userId },
  });

  check(res, {
    'deposit is 201 (first) or 409 (duplicate)': (r) => r.status === 201 || r.status === 409,
  });

  sleep(1);
}
