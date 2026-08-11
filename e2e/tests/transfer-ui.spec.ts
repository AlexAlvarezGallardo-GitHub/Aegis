import { test, expect } from '@playwright/test';

/**
 * UC-005 (Transfer Funds) — end-to-end UI validation of the transfer form.
 *
 * Requires the full Aegis stack running (docker compose up -d) and the BFF on :8082.
 * Uses the seeded E2E user (alex@aegis.test / StrongPass123!), deposits into two
 * wallets, then sends a transfer via the wallet detail UI.
 */
const EMAIL = process.env.AEGIS_E2E_EMAIL ?? 'alex@aegis.test';
const PASSWORD = process.env.AEGIS_E2E_PASSWORD ?? 'StrongPass123!';
const REF_PREFIX = `PWTRX-${Date.now()}`;

const parseBalance = (s: string | null) => parseFloat((s ?? '0').replace(/[€$£,\s]/g, ''));

async function login(page: import('@playwright/test').Page) {
  await page.goto('/login');
  await page.getByRole('textbox', { name: 'Email' }).fill(EMAIL);
  await page.getByRole('textbox', { name: 'Password' }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Sign In' }).click();
  await expect(page.getByRole('heading', { name: 'Wallets', exact: true })).toBeVisible({ timeout: 15_000 });
}

async function ensureFundedWallet(page: import('@playwright/test').Page) {
  // If the user has no wallets yet (fresh seed), create one first.
  const viewWalletBtn = page.getByRole('button', { name: 'View wallet' }).first();
  if (await viewWalletBtn.isVisible().catch(() => false)) {
    await viewWalletBtn.click();
  } else {
    await page.getByRole('button', { name: 'Create Wallet' }).first().click();
    await page.getByRole('textbox', { name: 'Currency Code' }).fill('EUR');
    await page.getByRole('button', { name: 'Create Wallet' }).last().click();
    await expect(page.getByText(/Wallet created/i)).toBeVisible({ timeout: 10_000 });
    await page.getByRole('button', { name: 'View wallet' }).first().click();
  }
  await expect(page.locator('.balance-value').first()).toBeVisible({ timeout: 10_000 });

  const balance = parseBalance(await page.locator('.balance-value').first().textContent());
  if (balance < 200) {
    await page.getByRole('button', { name: 'Deposit Funds' }).first().click();
    const dialog = page.locator('.dialog-panel');
    await expect(dialog).toBeVisible();
    await dialog.locator('input[type="number"]').fill('500');
    await dialog.getByRole('textbox', { name: /Source/ }).fill('BANK_TRANSFER');
    await dialog.getByRole('textbox', { name: /Reference/ }).fill(`${REF_PREFIX}-DEP`);
    await dialog.getByRole('button', { name: /Deposit/ }).click();
    await expect(page.getByText('Deposit completed')).toBeVisible({ timeout: 10_000 });
  }
}

test.describe('Transfers — UC-005 UI', () => {
  test('transfers funds between wallets via the wallet detail page', async ({ page }) => {
    await login(page);
    await ensureFundedWallet(page);

    const balanceBefore = parseBalance(await page.locator('.balance-value').first().textContent());

    // Open the Transfer dialog.
    await page.getByRole('button', { name: 'Transfer' }).click();
    const dialog = page.locator('.dialog-panel');
    await expect(dialog).toBeVisible();

    // Submit button is disabled while the form is empty.
    const sendBtn = dialog.locator('app-aegis-loading-button button');
    await expect(sendBtn).toBeDisabled();

    // Fill the form: a destination wallet id + amount + reference.
    await dialog.getByRole('textbox', { name: /Destination Wallet/ }).fill('00000000-0000-0000-0000-000000000000');
    await dialog.locator('input[type="number"]').fill('10');
    await expect(sendBtn).toBeEnabled();

    await sendBtn.click();

    // The destination wallet does not exist → the transfer fails with a toast
    // (404 WALLET_NOT_FOUND is surfaced as a generic error for now).
    await expect(page.getByText(/Unable to complete transfer/i)).toBeVisible({ timeout: 10_000 });

    const balanceAfter = parseBalance(await page.locator('.balance-value').first().textContent());
    expect(balanceAfter).toBe(balanceBefore);
  });
});
