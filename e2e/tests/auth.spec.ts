import { test, expect } from '@playwright/test';

/**
 * UC-001 / UC-002 — Authentication
 * Requires the full Aegis stack running (docker compose up -d) and a registered user.
 */
const EMAIL = process.env.AEGIS_E2E_EMAIL ?? 'alex@aegis.test';
const PASSWORD = process.env.AEGIS_E2E_PASSWORD ?? 'StrongPass123!';

test.describe('Authentication', () => {
  test('login form validates and signs in', async ({ page }) => {
    await page.goto('/login');

    await expect(page.getByRole('heading', { name: 'Sign In' })).toBeVisible();

    // Sign In is disabled while the form is empty
    const signIn = page.getByRole('button', { name: 'Sign In' });
    await expect(signIn).toBeDisabled();

    await page.getByRole('textbox', { name: 'Email' }).fill(EMAIL);
    await page.getByRole('textbox', { name: 'Password' }).fill(PASSWORD);

    await expect(signIn).toBeEnabled();
    await signIn.click();

    // AuthGuard redirects to the app shell
    await expect(page).toHaveURL(/\/dashboard|\/wallets/);
    await expect(page.getByRole('navigation')).toBeVisible();
  });

  test('rejects invalid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('textbox', { name: 'Email' }).fill(EMAIL);
    await page.getByRole('textbox', { name: 'Password' }).fill('wrong-password');
    await page.getByRole('button', { name: 'Sign In' }).click();

    // Expect a toast / error surfaced by the auth flow
    await expect(page.getByText(/invalid|error|credenciales|incorrect/i).first()).toBeVisible({ timeout: 10_000 });
  });
});
