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
  await expect(page.getByRole('heading', { name: 'Wallets' })).toBeVisible({ timeout: 15_000 });
}

test.describe('Wallet — UC-003 / UC-004', () => {
  test('creates a wallet', async ({ page }) => {
    await login(page);

    const before = await page.locator('text=Total Wallets').locator('..').getByText(/^\d+$/).first().textContent();
    const countBefore = before ? parseInt(before, 10) : 0;

    await page.getByRole('button', { name: 'Create Wallet' }).click();
    await page.getByRole('textbox', { name: 'Currency Code' }).fill('GBP');
    await page.getByRole('button', { name: 'Create Wallet' }).last().click();

    await expect(page.getByText(/Wallet created/i)).toBeVisible({ timeout: 10_000 });

    // Stats should reflect the new wallet
    await expect(page.getByText('Total Wallets').locator('..').getByText(String(countBefore + 1))).toBeVisible();
  });

  test('deposits funds with source and reference (UC-004)', async ({ page }) => {
    await login(page);

    // Open the first wallet's detail panel
    await page.getByRole('button', { name: 'View wallet' }).first().click();
    await expect(page.getByRole('heading', { name: 'Wallet Detail' })).toBeVisible();

    const balanceBefore = await page.locator('text=Balance').locator('..').getByText(/€|\$|£/).first().textContent();

    // Open the dedicated Deposit Funds form
    await page.getByRole('button', { name: 'Deposit Funds' }).click();
    await page.locator('input[type="number"]').first().fill('150');
    await page.getByRole('textbox', { name: 'Source' }).fill('BANK_TRANSFER');
    await page.getByRole('textbox', { name: 'Reference' }).fill(`${REF_PREFIX}-DEP`);
    await page.getByRole('button', { name: 'Deposit' }).click();

    // Receipt with source + reference is shown
    await expect(page.getByText(`Last deposit:`)).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(`${REF_PREFIX}-DEP`)).toBeVisible();

    // Balance increased (text changed from the captured value)
    const balanceAfter = await page.locator('text=Balance').locator('..').getByText(/€|\$|£/).first().textContent();
    expect(balanceAfter).not.toBe(balanceBefore);
  });

  test('rejects duplicate deposit reference (idempotency → 409)', async ({ page }) => {
    await login(page);

    await page.getByRole('button', { name: 'View wallet' }).first().click();
    await expect(page.getByRole('heading', { name: 'Wallet Detail' })).toBeVisible();

    const balanceBefore = await page.locator('text=Balance').locator('..').getByText(/€|\$|£/).first().textContent();

    const ref = `${REF_PREFIX}-DUP`;
    for (let i = 0; i < 2; i++) {
      await page.getByRole('button', { name: 'Deposit Funds' }).click();
      await page.locator('input[type="number"]').first().fill('50');
      await page.getByRole('textbox', { name: 'Source' }).fill('CARD');
      await page.getByRole('textbox', { name: 'Reference' }).fill(ref);
      await page.getByRole('button', { name: 'Deposit' }).click();
      await page.waitForTimeout(1_500);
    }

    // Second attempt with the same reference must not change the balance
    await expect(page.getByText(/Duplicate deposit reference|Failed to deposit/i)).toBeVisible({ timeout: 10_000 });
    const balanceAfter = await page.locator('text=Balance').locator('..').getByText(/€|\$|£/).first().textContent();
    expect(balanceAfter).toBe(balanceBefore);
  });
});
