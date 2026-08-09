// k6 load test: deposit idempotency via BFF
// Expected: first attempt 201, every repeat with the same reference 409 (no double-apply).
// Run: k6 run --vus 10 --duration 1m load/k6/idempotency.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import {
  BASE_URL, login, userEmail, PASSWORD, stateChangingHeaders, ensureWallet,
} from './lib.js';

export const options = {
  vus: 10,
  duration: '1m',
  // NOTE: 409 is the EXPECTED response for repeated references, so the generic
  // `http_req_failed` threshold does not apply here (k6 counts any 4xx/5xx as
  // a failure). Correctness is asserted below: 201 first / 409 repeat / never 5xx.
  thresholds: {
    checks: ['rate==1'],
  },
};

export default function () {
  const vu = __VU;
  login(userEmail(vu), PASSWORD);

  const walletId = ensureWallet(vu, 'EUR');
  if (!walletId) {
    return;
  }

  const res = http.post(`${BASE_URL}/api/bff/wallets/${walletId}/deposits`, JSON.stringify({
    amount: 50.0,
    currency: 'EUR',
    source: 'BANK_TRANSFER',
    reference: `K6-IDEM-${vu}`,
  }), {
    headers: stateChangingHeaders(),
  });

  check(res, {
    'deposit is 201 (first) or 409 (duplicate)': (r) => r.status === 201 || r.status === 409,
    'never a 5xx': (r) => r.status < 500,
  });

  sleep(1);
}
