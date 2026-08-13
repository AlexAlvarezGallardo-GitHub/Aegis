import { test, expect } from '@playwright/test';

/**
 * UC-007 (Refund Payment) — end-to-end saga validation via the service APIs.
 *
 * Requires the full Aegis stack running (docker compose up -d). The refund
 * endpoint ships with UC-007.
 */

const BASE_IDENTITY = process.env.AEGIS_E2E_IDENTITY_URL ?? 'http://127.0.0.1:8081';
const BASE_WALLET = process.env.AEGIS_E2E_WALLET_URL ?? 'http://127.0.0.1:8083';
const BASE_PAYMENT = process.env.AEGIS_E2E_PAYMENT_URL ?? 'http://127.0.0.1:8084';

const PASSWORD = 'StrongPass123!';

let seed = 0;
const stamp = Date.now();

function uniq(prefix: string) {
  seed += 1;
  return `${prefix}-${stamp}-${seed}@aegis.test`;
}

async function registerUser(request: import('@playwright/test').APIRequestContext, email: string) {
  const res = await request.post(`${BASE_IDENTITY}/api/v1/users/register`, {
    data: { email, password: PASSWORD, firstName: 'E2E', lastName: 'Refund' },
  });
  expect(res.status(), `register user ${email}`).toBe(201);
  const body = await res.json();
  return body.userId ?? body.id ?? body;
}

async function createWallet(request: import('@playwright/test').APIRequestContext, userId: string, currency: string) {
  const res = await request.post(`${BASE_WALLET}/api/v1/wallets`, {
    headers: { 'X-User-Id': userId },
    data: { currency },
  });
  expect(res.status(), `create wallet ${currency}`).toBe(201);
  return (await res.json()).walletId as string;
}

async function getWallet(request: import('@playwright/test').APIRequestContext, userId: string, walletId: string) {
  const res = await request.get(`${BASE_WALLET}/api/v1/wallets/${walletId}`, {
    headers: { 'X-User-Id': userId },
  });
  expect(res.status(), `get wallet ${walletId}`).toBe(200);
  return res.json();
}

async function deposit(request: import('@playwright/test').APIRequestContext, userId: string, walletId: string, amount: number, reference: string) {
  const res = await request.post(`${BASE_WALLET}/api/v1/wallets/${walletId}/deposits`, {
    headers: { 'X-User-Id': userId },
    data: { amount, currency: 'EUR', source: 'BANK_TRANSFER', reference },
  });
  expect(res.status(), `deposit ${amount} into ${walletId}`).toBe(201);
}

async function pay(
  request: import('@playwright/test').APIRequestContext,
  walletId: string,
  userId: string,
  amount: number,
  reference: string,
) {
  const res = await request.post(`${BASE_PAYMENT}/api/v1/payments`, {
    headers: { 'X-User-Id': userId },
    data: {
      walletId,
      amount,
      currency: 'EUR',
      payee: { name: 'Cafe Central', id: 'merchant-123', type: 'MERCHANT' },
      description: 'E2E payment',
      reference,
    },
  });
  expect(res.status(), 'payment returns 201').toBe(201);
  return (await res.json()).paymentId as string;
}

async function refund(
  request: import('@playwright/test').APIRequestContext,
  paymentId: string,
  userId: string,
  reference: string,
  amount?: number,
  reason?: string,
) {
  return request.post(`${BASE_PAYMENT}/api/v1/payments/${paymentId}/refund`, {
    headers: { 'X-User-Id': userId },
    data: {
      amount,
      reason,
      reference,
    },
  });
}

test.describe('Refunds — UC-007 saga (API-level)', () => {
  test('refunds a completed payment in full: balance restored', async ({ request }) => {
    const email = uniq('ref');
    const userId = await registerUser(request, email);
    const walletId = await createWallet(request, userId, 'EUR');
    await deposit(request, userId, walletId, 500.0, `${email}-dep`);

    const paymentId = await pay(request, walletId, userId, 25.0, `${email}-pay`);
    const before = await getWallet(request, userId, walletId);
    expect(Number(before.balance)).toBe(475.0);

    const res = await refund(request, paymentId, userId, `${email}-ref-1`, 25.0, 'Product returned');
    expect(res.status(), 'refund returns 200').toBe(200);
    const body = await res.json();
    expect(body.status).toBe('COMPLETED');
    expect(body.paymentId).toBe(paymentId);
    expect(Number(body.amount)).toBe(25.0);
    expect(Number(body.newBalance)).toBe(500.0);

    const after = await getWallet(request, userId, walletId);
    expect(Number(after.balance)).toBe(500.0);
  });

  test('rejects a duplicate refund reference (idempotent → same refund, no double credit)', async ({ request }) => {
    const email = uniq('refdup');
    const userId = await registerUser(request, email);
    const walletId = await createWallet(request, userId, 'EUR');
    await deposit(request, userId, walletId, 300.0, `${email}-dep`);

    const paymentId = await pay(request, walletId, userId, 20.0, `${email}-pay`);
    const reference = `${email}-ref-dup`;

    const first = await refund(request, paymentId, userId, reference, 20.0);
    expect(first.status(), 'first refund succeeds').toBe(200);

    const afterFirst = await getWallet(request, userId, walletId);

    const second = await refund(request, paymentId, userId, reference, 20.0);
    expect(second.status(), 'duplicate refund returns the existing refund').toBe(200);

    const afterSecond = await getWallet(request, userId, walletId);
    expect(Number(afterSecond.balance), 'no double credit on duplicate').toBe(Number(afterFirst.balance));
  });

  test('rejects refunding an already-refunded payment (409)', async ({ request }) => {
    const email = uniq('ref409');
    const userId = await registerUser(request, email);
    const walletId = await createWallet(request, userId, 'EUR');
    await deposit(request, userId, walletId, 300.0, `${email}-dep`);

    const paymentId = await pay(request, walletId, userId, 20.0, `${email}-pay`);
    await refund(request, paymentId, userId, `${email}-ref-a`, 20.0);
    expect((await refund(request, paymentId, userId, `${email}-ref-b`, 20.0)).status(), 'second refund on refunded payment → 409').toBe(409);
  });

  test('rejects a refund that exceeds the payment amount (422)', async ({ request }) => {
    const email = uniq('refex');
    const userId = await registerUser(request, email);
    const walletId = await createWallet(request, userId, 'EUR');
    await deposit(request, userId, walletId, 300.0, `${email}-dep`);

    const paymentId = await pay(request, walletId, userId, 20.0, `${email}-pay`);
    const res = await refund(request, paymentId, userId, `${email}-ref-ex`, 99.0);
    expect(res.status(), 'refund exceeds payment → 422').toBe(422);
  });

  test('rejects refunding a payment owned by another user (403)', async ({ request }) => {
    const ownerEmail = uniq('refown');
    const ownerId = await registerUser(request, ownerEmail);
    const walletId = await createWallet(request, ownerId, 'EUR');
    await deposit(request, ownerId, walletId, 300.0, `${ownerEmail}-dep`);
    const paymentId = await pay(request, walletId, ownerId, 20.0, `${ownerEmail}-pay`);

    const otherEmail = uniq('refother');
    const otherId = await registerUser(request, otherEmail);
    const res = await refund(request, paymentId, otherId, `${otherEmail}-ref`);
    expect(res.status(), 'refund of another user\'s payment → 403').toBe(403);
  });
});
