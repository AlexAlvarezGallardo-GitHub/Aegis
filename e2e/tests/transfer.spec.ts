import { test, expect } from '@playwright/test';

/**
 * UC-005 (Transfer Funds) — end-to-end saga validation via the service APIs.
 *
 * Exercises the full orchestrated saga without the UI (the transfer form ships
 * in #253): identity register → 2 wallets → deposits → transfer
 * (fraud check → hold → atomic settle) → balance assertions.
 *
 * Requires the full Aegis stack running (docker compose up -d).
 * The payment service runs with the `dev` profile (permit-all) in the sandbox.
 */

const BASE_IDENTITY = process.env.AEGIS_E2E_IDENTITY_URL ?? 'http://localhost:8081';
const BASE_WALLET = process.env.AEGIS_E2E_WALLET_URL ?? 'http://localhost:8083';
const BASE_PAYMENT = process.env.AEGIS_E2E_PAYMENT_URL ?? 'http://localhost:8084';

const PASSWORD = 'StrongPass123!';

let seed = 0;
const stamp = Date.now();

function uniq(prefix: string) {
  seed += 1;
  return `${prefix}-${stamp}-${seed}@aegis.test`;
}

interface WalletRef {
  walletId: string;
  userId: string;
}

async function registerUser(request: import('@playwright/test').APIRequestContext, email: string) {
  const res = await request.post(`${BASE_IDENTITY}/api/v1/users/register`, {
    data: {
      email,
      password: PASSWORD,
      firstName: 'E2E',
      lastName: 'Transfer',
    },
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
  const body = await res.json();
  return { walletId: body.walletId, userId };
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
    data: {
      amount,
      currency: 'EUR',
      source: 'BANK_TRANSFER',
      reference,
    },
  });
  expect(res.status(), `deposit ${amount} into ${walletId}`).toBe(201);
}

async function transfer(
  request: import('@playwright/test').APIRequestContext,
  src: WalletRef,
  dst: WalletRef,
  amount: number,
  reference: string,
) {
  const res = await request.post(`${BASE_PAYMENT}/api/v1/transfers`, {
    data: {
      sourceWalletId: src.walletId,
      destWalletId: dst.walletId,
      userId: src.userId,
      amount,
      currency: 'EUR',
      description: 'E2E transfer',
      reference,
    },
  });
  return res;
}

test.describe('Transfers — UC-005 saga (API-level)', () => {
  test('transfers funds between wallets: fraud → hold → atomic settle', async ({ request }) => {
    const email = uniq('trx');
    const userId = await registerUser(request, email);

    const src = await createWallet(request, userId, 'EUR');
    const dst = await createWallet(request, userId, 'EUR');

    await deposit(request, src.userId, src.walletId, 500.0, `${email}-dep-src`);
    await deposit(request, dst.userId, dst.walletId, 100.0, `${email}-dep-dst`);

    const srcBefore = await getWallet(request, src.userId, src.walletId);
    const dstBefore = await getWallet(request, dst.userId, dst.walletId);

    const res = await transfer(request, src, dst, 150.0, `${email}-trx-1`);

    expect(res.status(), 'transfer returns 201').toBe(201);
    const body = await res.json();
    expect(body.status, 'transfer completes synchronously').toBe('COMPLETED');
    expect(body.transferId).toBeTruthy();
    expect(body.holdId, 'hold id recorded on transfer').toBeTruthy();
    expect(body.reference).toBe(`${email}-trx-1`);

    const srcAfter = await getWallet(request, src.userId, src.walletId);
    const dstAfter = await getWallet(request, dst.userId, dst.walletId);

    expect(Number(srcAfter.balance)).toBe(Number(srcBefore.balance) - 150);
    expect(Number(dstAfter.balance)).toBe(Number(dstBefore.balance) + 150);
  });

  test('rejects duplicate transfer reference (idempotency → 409)', async ({ request }) => {
    const email = uniq('dup');
    const userId = await registerUser(request, email);

    const src = await createWallet(request, userId, 'EUR');
    const dst = await createWallet(request, userId, 'EUR');

    await deposit(request, src.userId, src.walletId, 300.0, `${email}-dep-src`);
    await deposit(request, dst.userId, dst.walletId, 50.0, `${email}-dep-dst`);

    const reference = `${email}-dup-trx`;
    const first = await transfer(request, src, dst, 75.0, reference);
    expect(first.status(), 'first transfer succeeds').toBe(201);

    const srcAfterFirst = await getWallet(request, src.userId, src.walletId);

    const second = await transfer(request, src, dst, 75.0, reference);
    expect(second.status(), 'duplicate reference rejected with 409').toBe(409);

    const srcAfterSecond = await getWallet(request, src.userId, src.walletId);
    expect(Number(srcAfterSecond.balance), 'no double debit on duplicate').toBe(Number(srcAfterFirst.balance));
  });

  test('fails closed when source has insufficient funds (422)', async ({ request }) => {
    const email = uniq('insf');
    const userId = await registerUser(request, email);

    const src = await createWallet(request, userId, 'EUR');
    const dst = await createWallet(request, userId, 'EUR');

    await deposit(request, src.userId, src.walletId, 50.0, `${email}-dep-src`);
    await deposit(request, dst.userId, dst.walletId, 10.0, `${email}-dep-dst`);

    const srcBefore = await getWallet(request, src.userId, src.walletId);
    const dstBefore = await getWallet(request, dst.userId, dst.walletId);

    const res = await transfer(request, src, dst, 500.0, `${email}-trx-insf`);

    expect(res.status(), 'insufficient funds → 422').toBe(422);
    const body = await res.json();
    expect(body.code).toBe('INSUFFICIENT_FUNDS');

    const srcAfter = await getWallet(request, src.userId, src.walletId);
    const dstAfter = await getWallet(request, dst.userId, dst.walletId);
    expect(Number(srcAfter.balance)).toBe(Number(srcBefore.balance));
    expect(Number(dstAfter.balance)).toBe(Number(dstBefore.balance));
  });

  test('rejects self-transfer (same source and destination → 422/400)', async ({ request }) => {
    const email = uniq('self');
    const userId = await registerUser(request, email);
    const wallet = await createWallet(request, userId, 'EUR');
    await deposit(request, wallet.userId, wallet.walletId, 100.0, `${email}-dep-self`);

    const res = await transfer(request, wallet, wallet, 25.0, `${email}-trx-self`);
    expect([400, 422]).toContain(res.status());
  });
});
