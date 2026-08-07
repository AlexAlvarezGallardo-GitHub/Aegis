// k6 load test: BFF login flow
// Run: k6 run --vus 50 --duration 2m load/k6/login.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<300'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';

export default function () {
  const res = http.post(`${BASE_URL}/api/bff/auth/login`, JSON.stringify({
    email: `user${__VU}@example.com`,
    password: 's3cret',
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'login status is 200': (r) => r.status === 200,
    'response has session cookie': (r) => r.cookies['JSESSIONID'] !== undefined,
  });

  sleep(1);
}
