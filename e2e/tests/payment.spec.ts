import { test, expect } from '@playwright/test';

/**
 * UC-006 (Execute Payment) — end-to-end saga validation via the service APIs.
 *
 * Exercises the full payment saga without the UI (the payment form ships in
 * #266): identity register → wallet → deposit → payment
 * (fraud check → hold → atomic debit) → balance assertions.
 *
 * Requires the full Aegis stack running (docker compose up -d).
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
    data: { email, password: PASSWORD, firstName: 'E2E', lastName: 'Payment' },
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
  return request.post(`${BASE_PAYMENT}/api/v1/payments`, {
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
}

test.describe('Payments — UC-006 saga (API-level)', () => {
  test('executes a payment to a merchant: fraud → hold → atomic debit', async ({ request }) => {
    const email = uniq('pay');
    const userId = await registerUser(request, email);
    const walletId = await createWallet(request, userId, 'EUR');
    await deposit(request, userId, walletId, 500.0, `${email}-dep`);

    const before = await getWallet(request, userId, walletId);

    const res = await pay(request, walletId, userId, 25.0, `${email}-pay-1`);
    expect(res.status(), 'payment returns 201').toBe(201);
    const body = await res.json();
    expect(body.status, 'payment completes synchronously').toBe('COMPLETED');
    expect(body.paymentId).toBeTruthy();
    expect(body.holdId, 'hold id recorded on payment').toBeTruthy();
    expect(body.reference).toBe(`${email}-pay-1`);
    expect(body.payee.name).toBe('Cafe Central');

    const after = await getWallet(request, userId, walletId);
    expect(Number(after.balance)).toBe(Number(before.balance) - 25);
  });

  test('rejects duplicate payment reference (idempotency → 409)', async ({ request }) => {
    const email = uniq('paydup');
    const userId = await registerUser(request, email);
    const walletId = await createWallet(request, userId, 'EUR');
    await deposit(request, userId, walletId, 300.0, `${email}-dep`);

    const reference = `${email}-dup-pay`;
    const first = await pay(request, walletId, userId, 10.0, reference);
    expect(first.status(), 'first payment succeeds').toBe(201);

    const afterFirst = await getWallet(request, userId, walletId);

    const second = await pay(request, walletId, userId, 10.0, reference);
    expect(second.status(), 'duplicate reference rejected with 409').toBe(409);

    const afterSecond = await getWallet(request, userId, walletId);
    expect(Number(afterSecond.balance), 'no double debit on duplicate').toBe(Number(afterFirst.balance));
  });

  test('fails closed when the wallet has insufficient funds (422)', async ({ request }) => {
    const email = uniq('payinsf');
    const userId = await registerUser(request, email);
    const walletId = await createWallet(request, userId, 'EUR');
    await deposit(request, userId, walletId, 50.0, `${email}-dep`);

    const before = await getWallet(request, userId, walletId);

    const res = await pay(request, walletId, userId, 500.0, `${email}-pay-insf`);
    expect(res.status(), 'insufficient funds → 422').toBe(422);
    const body = await res.json();
    expect(body.code).toBe('INSUFFICIENT_FUNDS');

    const after = await getWallet(request, userId, walletId);
    expect(Number(after.balance)).toBe(Number(before.balance));
  });
});
