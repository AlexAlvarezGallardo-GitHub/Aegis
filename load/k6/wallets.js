// k6 load test: wallet creation + listing
// Run: k6 run --vus 30 --duration 2m load/k6/wallets.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 30 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<300'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';

export default function () {
  const userId = `user-${__VU}-${__ITER}`;

  const create = http.post(`${BASE_URL}/api/bff/wallets`, JSON.stringify({
    currency: 'EUR',
  }), {
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': userId,
    },
  });

  check(create, {
    'create wallet status is 201': (r) => r.status === 201,
  });

  const list = http.get(`${BASE_URL}/api/bff/wallets`, {
    headers: { 'X-User-Id': userId },
  });

  check(list, {
    'list wallets status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
