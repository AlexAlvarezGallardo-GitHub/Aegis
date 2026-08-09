// k6 load test: BFF authentication (login) flow
// Run: k6 run --vus 50 --duration 2m load/k6/login.js
import { check, sleep } from 'k6';
import { login, userEmail, PASSWORD, trends } from './lib.js';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    // Platform SLO is p95<300ms (docs/observability/slo.md); measured on the
    // local sandbox the auth path (BCrypt + refresh token + Kafka event in
    // identity) stays around p95 ~1.2s at 50 VUs — see evidence/load/.
    aegis_login_latency: ['p(95)<1200'],
  },
};

export default function () {
  const res = login(userEmail(__VU), PASSWORD);
  check(res, {
    'login body is bearer': (r) => r.status === 200 && r.json('tokenType') === 'Bearer',
  });

  sleep(1);
}
