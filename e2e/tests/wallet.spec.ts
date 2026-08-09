import { test, expect } from '@playwright/test';

/**
 * UC-003 (Create Wallet) and UC-004 (Deposit Funds) — Wallet flows.
 *
 * Requires the full Aegis stack running and a logged-in session.
 * Each test logs in itself so they are independent.
 */
const EMAIL = process.env.AEGIS_E2E_EMAIL ?? 'alex@aegis.test';
const PASSWORD = process.env.AEGIS_E2E_PASSWORD ?? 'StrongPass123!';
const REF_PREFIX = `PW-${Date.now()}`;

async function login(page: import('@playwright/test').Page) {
  await page.goto('/login');
  await page.getByRole('textbox', { name: 'Email' }).fill(EMAIL);
  await page.getByRole('textbox', { name: 'Password' }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Sign In' }).click();
  await expect(page.getByRole('heading', { name: 'Wallets', exact: true })).toBeVisible({ timeout: 15_000 });
}

test.describe('Wallet — UC-003 / UC-004', () => {
  test('creates a wallet', async ({ page }) => {
    await login(page);

    const stat = page.locator('.stat-card').filter({ hasText: 'Total Wallets' });
    const before = parseInt((await stat.locator('.stat-value').textContent()) ?? '0', 10);

    await page.getByRole('button', { name: 'Create Wallet' }).first().click();
    await page.getByRole('textbox', { name: 'Currency Code' }).fill('GBP');
    await page.getByRole('button', { name: 'Create Wallet' }).last().click();

    await expect(page.getByText(/Wallet created/i)).toBeVisible({ timeout: 10_000 });

    // Stats should reflect the new wallet and a GBP card should be in the grid
    await expect(stat.locator('.stat-value')).toHaveText(String(before + 1));
    await expect(page.locator('.wallet-card').filter({ hasText: 'GBP' }).first()).toBeVisible();
  });

  test('deposits funds with source and reference (UC-004)', async ({ page }) => {
    await login(page);

    // Open the first wallet's detail panel
    await page.getByRole('button', { name: 'View wallet' }).first().click();
    await expect(page.getByRole('heading', { name: 'Wallet Detail' })).toBeVisible();

    const balanceBefore = await page.locator('text=Balance').locator('..').getByText(/€|\$|£/).first().textContent();

    // Open the dedicated Deposit Funds form
    await page.getByRole('button', { name: 'Deposit Funds' }).click();
    await page.locator('.deposit-form input[type="number"]').fill('150');
    await page.getByRole('textbox', { name: 'Source' }).fill('BANK_TRANSFER');
    await page.getByRole('textbox', { name: 'Reference' }).fill(`${REF_PREFIX}-DEP`);
    await page.locator('.slide-panel').getByRole('button', { name: 'Deposit', exact: true }).click();

    // Receipt with source + reference is shown
    await expect(page.getByText(`Last deposit:`)).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(`${REF_PREFIX}-DEP`).first()).toBeVisible();

    // Balance increased (text changed from the captured value)
    const balanceAfter = await page.locator('text=Balance').locator('..').getByText(/€|\$|£/).first().textContent();
    expect(balanceAfter).not.toBe(balanceBefore);
  });

  test('rejects duplicate deposit reference (idempotency → 409)', async ({ page }) => {
    await login(page);

    await page.getByRole('button', { name: 'View wallet' }).first().click();
    await expect(page.getByRole('heading', { name: 'Wallet Detail' })).toBeVisible();

    const parseBalance = (s: string | null) => parseFloat((s ?? '0').replace(/[€$£,\s]/g, ''));
    const balanceBefore = parseBalance(await page.locator('text=Balance').locator('..').getByText(/€|\$|£/).first().textContent());

    const ref = `${REF_PREFIX}-DUP`;
    for (let i = 0; i < 2; i++) {
      await page.getByRole('button', { name: 'Deposit Funds' }).click();
      await page.locator('.deposit-form input[type="number"]').fill('50');
      await page.getByRole('textbox', { name: 'Source' }).fill('CARD');
      await page.getByRole('textbox', { name: 'Reference' }).fill(ref);
      await page.locator('.slide-panel').getByRole('button', { name: 'Deposit', exact: true }).click();
      await page.waitForTimeout(1_500);
    }

    // The second attempt with the same reference must be rejected (409)
    await expect(page.getByText(/Duplicate deposit reference|Failed to deposit/i)).toBeVisible({ timeout: 10_000 });
    // Exactly ONE of the two identical deposits applied (idempotency)
    const balanceAfter = parseBalance(await page.locator('text=Balance').locator('..').getByText(/€|\$|£/).first().textContent());
    expect(balanceAfter).toBe(balanceBefore + 50);
  });
});
