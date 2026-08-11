// Shared helpers for Aegis k6 scenarios.
//
// The BFF uses:
//   - a server-side session cookie named `SESSION` (set by POST /api/bff/auth/login)
//   - cookie-based CSRF: the `XSRF-TOKEN` cookie ROTATES on every state-changing
//     request, so the header must be re-read from the cookie jar before each call.
//   - a Bearer JWT stored server-side in the session (no client-side token needed).
//
// Each iteration authenticates with a FRESH session (login per iteration). Reusing
// a session across iterations is unreliable with k6's cookie jar because the BFF
// rotates the SESSION cookie (session fixation protection) on state-changing calls.
//
// Endpoint latencies are recorded in custom Trend metrics so scenarios can set
// SLO thresholds per endpoint without mixing the auth path into the business path.
import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';

export const PASSWORD = 'LoadTest123!';

export const trends = {
  login: new Trend('aegis_login_latency'),
  createWallet: new Trend('aegis_create_wallet_latency'),
  listWallets: new Trend('aegis_list_wallets_latency'),
  deposit: new Trend('aegis_deposit_latency'),
  transfer: new Trend('aegis_transfer_latency'),
};

// Fresh user pool per load run; set USER_PREFIX to rotate seeds between runs.
export function userEmail(vu, pool = 60) {
  const prefix = __ENV.USER_PREFIX || 'k6user';
  return `${prefix}${((vu - 1) % pool) + 1}@aegis.test`;
}

/**
 * Authenticates the given user.
 */
export function login(email, password) {
  const res = http.post(`${BASE_URL}/api/bff/auth/login`, JSON.stringify({ email, password }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'login' },
  });
  trends.login.add(res.timings.duration);

  check(res, {
    'login returns 200': (r) => r.status === 200,
    'login sets session cookie': (r) => r.cookies['SESSION'] !== undefined,
  });
  return res;
}

/**
 * Returns state-changing headers with the CURRENT XSRF token from the jar.
 * Must be called right before the request (the token rotates per request).
 */
export function stateChangingHeaders() {
  const jar = http.cookieJar();
  const cookies = jar.cookiesForURL(BASE_URL);
  const headers = { 'Content-Type': 'application/json' };
  if (cookies['XSRF-TOKEN']) {
    headers['X-XSRF-TOKEN'] = cookies['XSRF-TOKEN'];
  }
  return headers;
}

/**
 * Creates a wallet for the given VU exactly once (cached by VU index).
 * Must be called after login in the same iteration. Returns the wallet id.
 */
export function ensureWallet(vu, currency = 'EUR') {
  const key = `w-${vu}`;
  if (ensureWallet._cache[key]) {
    return ensureWallet._cache[key];
  }
  const res = http.post(
    `${BASE_URL}/api/bff/wallets`,
    JSON.stringify({ currency }),
    { headers: stateChangingHeaders(), tags: { name: 'create_wallet' } },
  );
  trends.createWallet.add(res.timings.duration);

  check(res, {
    'create wallet returns 201': (r) => r.status === 201,
  });
  const walletId = res.status === 201 ? res.json('walletId') : undefined;
  ensureWallet._cache[key] = walletId;
  return walletId;
}
ensureWallet._cache = {};
