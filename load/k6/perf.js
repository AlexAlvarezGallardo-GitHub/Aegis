// k6 performance smoke for CI perf regression checks (Fase 3).
// Emits aegis_login_latency / aegis_list_wallets_latency / aegis_deposit_latency
// trends (see lib.js) so scripts/perf-check.py can compare p95 against a baseline.
// Run: docker run --rm -v "${PWD}\load:/scripts" grafana/k6:latest run \
//        --summary-export /scripts/out.json /scripts/k6/perf.js -e BASE_URL=...
import http from 'k6/http';
import { check, sleep } from 'k6';
import {
  BASE_URL, login, userEmail, PASSWORD, stateChangingHeaders, ensureWallet, trends,
} from './lib.js';

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const vu = __VU;
  login(userEmail(vu), PASSWORD);

  const walletId = ensureWallet(vu, 'EUR');
  if (!walletId) {
    return;
  }

  const deposit = http.post(`${BASE_URL}/api/bff/wallets/${walletId}/deposits`, JSON.stringify({
    amount: 10.0,
    currency: 'EUR',
    source: 'BANK_TRANSFER',
    reference: `K6-PERF-${vu}-${__ITER}-${Date.now()}`,
  }), {
    headers: stateChangingHeaders(),
    tags: { name: 'deposit' },
  });
  trends.deposit.add(deposit.timings.duration);
  check(deposit, {
    'deposit returns 201': (r) => r.status === 201,
  });

  const list = http.get(`${BASE_URL}/api/bff/wallets`, {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'list_wallets' },
  });
  trends.listWallets.add(list.timings.duration);
  check(list, {
    'list wallets returns 200': (r) => r.status === 200,
  });

  sleep(0.5);
}
