// k6 load test: concurrent peer-to-peer transfers (UC-005) — direct service API.
//
// The BFF transfer endpoint ships with the UI (#253); this scenario drives the
// payment saga directly against the dev-profile services: create 2 wallets
// (X-User-Id header), deposit into both, then transfer between them. Each VU
// keeps its own wallet pair.
//
// Prereq: seed a user pool with ./seed-users.ps1 -Prefix trfuser -Count 60
//
// Run: docker run --rm --add-host=host.docker.internal:host-gateway \
//        -v "${PWD}\load:/scripts" grafana/k6:latest run \
//        --summary-export /scripts/out.json /scripts/k6/transfers.js \
//        -e USER_PREFIX=trfuser
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import encoding from 'k6/encoding';

const IDENTITY = __ENV.IDENTITY_URL || 'http://host.docker.internal:8081';
const WALLET = __ENV.WALLET_URL || 'http://host.docker.internal:8083';
const PAYMENT = __ENV.PAYMENT_URL || 'http://host.docker.internal:8084';

const PASSWORD = 'LoadTest123!';
const prefix = __ENV.USER_PREFIX || 'k6trf';

const transferLatency = new Trend('aegis_transfer_latency');

export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '1m', target: 15 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    // Saga includes fraud assess + hold + settle; generous budget (docs/observability/slo.md).
    aegis_transfer_latency: ['p(95)<1500'],
  },
};

function email(vu) {
  return `${prefix}${((vu - 1) % 60) + 1}@aegis.test`;
}

function jsonHeaders() {
  return { 'Content-Type': 'application/json' };
}

// Cache per-VU wallet pairs to avoid re-creating on every iteration.
const pairCache = {};

function ensurePair(vu) {
  const key = `p-${vu}`;
  if (pairCache[key]) {
    return pairCache[key];
  }

  // Resolve the user id from the identity service (dev profile, register is idempotent-ish).
  const userId = resolveUserId(vu);
  if (!userId) {
    return null;
  }

  // Create two EUR wallets via the wallet service (X-User-Id header).
  const headers = Object.assign(jsonHeaders(), { 'X-User-Id': userId });

  // Reuse existing EUR wallets when present (the pair is created once per user),
  // otherwise create them — avoids the WALLET_LIMIT_EXCEEDED (max 5/user) rule.
  const existing = http.get(`${WALLET}/api/v1/wallets`, { headers, tags: { name: 'list_wallets' } });
  const eur = existing.status === 200 && Array.isArray(existing.json())
    ? existing.json().filter((w) => w.currency === 'EUR')
    : [];
  let walletId1 = eur.length > 0 ? eur[0].walletId : null;
  let walletId2 = eur.length > 1 ? eur[1].walletId : null;

  if (!walletId1) {
    const w1 = http.post(`${WALLET}/api/v1/wallets`, JSON.stringify({ currency: 'EUR' }),
      { headers, tags: { name: 'create_wallet' } });
    if (w1.status === 201) {
      walletId1 = w1.json('walletId');
    }
  }
  if (!walletId2) {
    const w2 = http.post(`${WALLET}/api/v1/wallets`, JSON.stringify({ currency: 'EUR' }),
      { headers, tags: { name: 'create_wallet' } });
    if (w2.status === 201) {
      walletId2 = w2.json('walletId');
    }
  }
  if (!walletId1 || !walletId2) {
    return null;
  }

  pairCache[key] = { userId, walletId1, walletId2 };
  return pairCache[key];
}

// Funds both wallets of the pair with a fresh reference every iteration so a
// reused pair (from a previous run) never runs dry.
function fundPair(vu, pair) {
  const headers = Object.assign(jsonHeaders(), { 'X-User-Id': pair.userId });
  const ref = `K6-FND-${vu}-${__ITER}-${Date.now()}`;
  const d1 = http.post(`${WALLET}/api/v1/wallets/${pair.walletId1}/deposits`, JSON.stringify({
    amount: 100.0, currency: 'EUR', source: 'BANK_TRANSFER', reference: `${ref}-A`,
  }), { headers, tags: { name: 'deposit' } });
  const d2 = http.post(`${WALLET}/api/v1/wallets/${pair.walletId2}/deposits`, JSON.stringify({
    amount: 100.0, currency: 'EUR', source: 'BANK_TRANSFER', reference: `${ref}-B`,
  }), { headers, tags: { name: 'deposit' } });
  return d1.status === 201 && d2.status === 201;
}

// Resolves the user id for a VU, cached per VU (the id never changes).
// Login through the identity service and decode the JWT subject (the login
// response carries accessToken but no userId field). Falls back to a fresh
// registration when the account does not exist yet.
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

// Decodes the `sub` claim from a JWT without a library (base64url payload).
function subjectOf(token) {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }
    const payload = JSON.parse(encoding.b64decode(parts[1]).toString());
    return payload.sub || null;
  } catch (e) {
    return null;
  }
}

export default function () {
  const vu = __VU;
  const pair = ensurePair(vu);
  if (!pair) {
    return;
  }

  // Top up both wallets each iteration so the pair never runs dry.
  if (!fundPair(vu, pair)) {
    return;
  }

  const reference = `K6-TRFX-${vu}-${__ITER}-${Date.now()}`;
  const res = http.post(`${PAYMENT}/api/v1/transfers`, JSON.stringify({
    sourceWalletId: pair.walletId1,
    destWalletId: pair.walletId2,
    userId: pair.userId,
    amount: 50.0,
    currency: 'EUR',
    description: 'k6 transfer',
    reference,
  }), {
    headers: jsonHeaders(),
    tags: { name: 'transfer' },
  });
  transferLatency.add(res.timings.duration);

  check(res, {
    'transfer returns 201': (r) => r.status === 201,
    'transfer completes synchronously': (r) => r.status === 201 && r.json('status') === 'COMPLETED',
  });

  sleep(0.5);
}
