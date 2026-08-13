// k6 load test: concurrent payment refunds (UC-007) — direct service API.
//
// Each VU: fund a wallet, execute a merchant payment, then refund it.
//
// Prereq: seed a user pool with ./seed-users.ps1 -Prefix refuser -Count 60
//
// Run: docker run --rm --add-host=host.docker.internal:host-gateway \
//        -v "${PWD}\load:/scripts" grafana/k6:latest run \
//        --summary-export /scripts/out.json /scripts/k6/refunds.js \
//        -e USER_PREFIX=refuser
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import encoding from 'k6/encoding';

const IDENTITY = __ENV.IDENTITY_URL || 'http://host.docker.internal:8081';
const WALLET = __ENV.WALLET_URL || 'http://host.docker.internal:8083';
const PAYMENT = __ENV.PAYMENT_URL || 'http://host.docker.internal:8084';

const PASSWORD = 'LoadTest123!';
const prefix = __ENV.USER_PREFIX || 'k6ref';

const refundLatency = new Trend('aegis_refund_latency');

export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '1m', target: 15 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    aegis_refund_latency: ['p(95)<1500'],
  },
};

function email(vu) {
  return `${prefix}${((vu - 1) % 60) + 1}@aegis.test`;
}

function jsonHeaders() {
  return { 'Content-Type': 'application/json' };
}

const userIdCache = {};

function resolveUserId(vu) {
  const key = `u-${vu}`;
  if (userIdCache[key]) {
    return userIdCache[key];
  }
  const user = email(vu);
  const login = http.post(`${IDENTITY}/api/v1/auth/login`, JSON.stringify({
    email: user, password: PASSWORD,
  }), { headers: jsonHeaders() });

  let id = null;
  if (login.status === 200 && login.json('accessToken')) {
    id = subjectOf(login.json('accessToken'));
  } else {
    const reg = http.post(`${IDENTITY}/api/v1/users/register`, JSON.stringify({
      email: user, password: PASSWORD, firstName: 'Load', lastName: `User${vu}`,
    }), { headers: jsonHeaders() });
    if (reg.status === 201) {
      id = reg.json('userId') || reg.json('id');
    }
  }
  userIdCache[key] = id;
  return id;
}

function subjectOf(token) {
  try {
    const parts = token.split('.');
    const payload = JSON.parse(encoding.b64decode(parts[1]).toString());
    return payload.sub;
  } catch (e) {
    return null;
  }
}

const walletCache = {};

function ensureWallet(vu) {
  const key = `w-${vu}`;
  if (walletCache[key]) {
    return walletCache[key];
  }

  const userId = resolveUserId(vu);
  if (!userId) {
    return null;
  }
  const headers = Object.assign(jsonHeaders(), { 'X-User-Id': userId });

  const existing = http.get(`${WALLET}/api/v1/wallets`, { headers, tags: { name: 'list_wallets' } });
  const eur = existing.status === 200 && Array.isArray(existing.json())
    ? existing.json().filter((w) => w.currency === 'EUR')
    : [];
  let walletId = eur.length > 0 ? eur[0].walletId : null;
  if (!walletId) {
    const w = http.post(`${WALLET}/api/v1/wallets`, JSON.stringify({ currency: 'EUR' }),
      { headers, tags: { name: 'create_wallet' } });
    walletId = w.status === 201 ? w.json('walletId') : null;
  }
  if (!walletId) {
    return null;
  }

  const ref = `K6-REF-${vu}-${Date.now()}`;
  const d = http.post(`${WALLET}/api/v1/wallets/${walletId}/deposits`, JSON.stringify({
    amount: 500.0, currency: 'EUR', source: 'BANK_TRANSFER', reference: `${ref}-A`,
  }), { headers, tags: { name: 'deposit' } });
  if (d.status !== 201) {
    return null;
  }

  walletCache[key] = { userId, walletId };
  return walletCache[key];
}

export default function () {
  const vu = __VU;
  const wallet = ensureWallet(vu);
  if (!wallet) {
    return;
  }
  const headers = Object.assign(jsonHeaders(), { 'X-User-Id': wallet.userId });

  // Execute a payment to create a refundable source.
  const payRef = `K6-REFPAY-${vu}-${__ITER}-${Date.now()}`;
  const pay = http.post(`${PAYMENT}/api/v1/payments`, JSON.stringify({
    walletId: wallet.walletId,
    amount: 20.0,
    currency: 'EUR',
    payee: { name: 'K6 Merchant', id: `merchant-${vu}`, type: 'MERCHANT' },
    description: 'k6 payment',
    reference: payRef,
  }), { headers, tags: { name: 'payment' } });
  if (pay.status !== 201) {
    return;
  }
  const paymentId = pay.json('paymentId');

  // Refund it.
  const refRef = `K6-REFRF-${vu}-${__ITER}-${Date.now()}`;
  const res = http.post(`${PAYMENT}/api/v1/payments/${paymentId}/refund`, JSON.stringify({
    amount: 20.0,
    reason: 'k6 refund',
    reference: refRef,
  }), { headers, tags: { name: 'refund' } });
  refundLatency.add(res.timings.duration);

  check(res, {
    'refund returns 200': (r) => r.status === 200,
    'refund completes synchronously': (r) => r.status === 200 && r.json('status') === 'COMPLETED',
  });

  sleep(0.5);
}
