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

const parseBalance = (s: string | null) => parseFloat((s ?? '0').replace(/[€$£,\s]/g, ''));

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

  test('navigates to the full-page wallet detail (UC-003)', async ({ page }) => {
    await login(page);

    await page.getByRole('button', { name: 'View wallet' }).first().click();

    // Full page (not a drawer): URL changes and the page header is visible
    await expect(page).toHaveURL(/\/wallets\/[0-9a-f-]{8,}/);
    await expect(page.getByRole('heading', { name: /Wallet$/ })).toBeVisible({ timeout: 10_000 });

    // Balance overview is immediately visible
    await expect(page.locator('.balance-value').first()).toBeVisible();

    // Tabs are present
    await expect(page.getByRole('tab', { name: 'Overview' })).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Transactions' })).toBeVisible();

    // Back navigation returns to the list
    await page.getByRole('button', { name: 'Wallets' }).click();
    await expect(page).toHaveURL(/\/wallets$/);
  });

  test('deposits funds with source and reference (UC-004)', async ({ page }) => {
    await login(page);

    await page.getByRole('button', { name: 'View wallet' }).first().click();
    await expect(page.locator('.balance-value').first()).toBeVisible({ timeout: 10_000 });

    const balanceBefore = parseBalance(await page.locator('.balance-value').first().textContent());

    // Deposit opens a modal dialog (not an inline form)
    await page.getByRole('button', { name: 'Deposit Funds' }).first().click();
    const dialog = page.locator('.dialog-panel');
    await expect(dialog).toBeVisible();
    await dialog.locator('input[type="number"]').fill('150');
    await dialog.getByRole('textbox', { name: /Source/ }).fill('BANK_TRANSFER');
    await dialog.getByRole('textbox', { name: /Reference/ }).fill(`${REF_PREFIX}-DEP`);
    await dialog.getByRole('button', { name: /Deposit/ }).click();

    // Compact toast with two lines
    await expect(page.getByText('Deposit completed')).toBeVisible({ timeout: 10_000 });

    // Balance increased
    const balanceAfter = parseBalance(await page.locator('.balance-value').first().textContent());
    expect(balanceAfter).toBeGreaterThan(balanceBefore);
  });

  test('rejects duplicate deposit reference (idempotency → 409)', async ({ page }) => {
    await login(page);

    await page.getByRole('button', { name: 'View wallet' }).first().click();
    await expect(page.locator('.balance-value').first()).toBeVisible({ timeout: 10_000 });

    const balanceBefore = parseBalance(await page.locator('.balance-value').first().textContent());

    const ref = `${REF_PREFIX}-DUP`;
    for (let i = 0; i < 2; i++) {
      await page.getByRole('button', { name: 'Deposit Funds' }).first().click();
      const dialog = page.locator('.dialog-panel');
      await dialog.locator('input[type="number"]').fill('50');
      await dialog.getByRole('textbox', { name: /Source/ }).fill('CARD');
      await dialog.getByRole('textbox', { name: /Reference/ }).fill(ref);
      await dialog.getByRole('button', { name: /Deposit/ }).click();
      await page.waitForTimeout(1_500);
    }

    // The second attempt with the same reference must be rejected (409)
    await expect(page.getByText(/Duplicate deposit reference|Failed to deposit/i)).toBeVisible({ timeout: 10_000 });
    // Exactly ONE of the two identical deposits applied (idempotency)
    const balanceAfter = parseBalance(await page.locator('.balance-value').first().textContent());
    expect(balanceAfter).toBe(balanceBefore + 50);
  });
});
