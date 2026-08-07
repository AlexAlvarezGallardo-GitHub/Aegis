// k6 load test: concurrent deposits (UC-004)
// Run: k6 run --vus 20 --duration 2m load/k6/deposits.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '1m', target: 20 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<400'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';

// Each VU creates its own wallet once, then deposits against it
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
  const userId = `dep-user-${__VU}`;
  const walletId = ensureWallet(userId);
  if (!walletId) {
    return;
  }

  const reference = `TXN-${__VU}-${__ITER}-${Date.now()}`;
  const res = http.post(`${BASE_URL}/api/bff/wallets/${walletId}/deposits`, JSON.stringify({
    amount: 100.0,
    currency: 'EUR',
    source: 'BANK_TRANSFER',
    reference,
  }), {
    headers: { 'Content-Type': 'application/json', 'X-User-Id': userId },
  });

  check(res, {
    'deposit status is 201': (r) => r.status === 201,
    'deposit returns new balance': (r) => r.json('newBalance') !== undefined,
  });

  sleep(1);
}
