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

    // The login page title is a mat-card-title (not a heading role), so we
    // assert on a stable form control instead.
    await expect(page.getByRole('textbox', { name: 'Email' })).toBeVisible();

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
    // Use a non-existent account so the failed attempt does not accumulate
    // failed-login counters on the real E2E user (account lockout).
    await page.getByRole('textbox', { name: 'Email' }).fill('nobody@aegis.test');
    await page.getByRole('textbox', { name: 'Password' }).fill('wrong-password');
    const signIn = page.getByRole('button', { name: 'Sign In' });
    await signIn.click();

    // The request completes with 401: the submit button re-enables (isLoading
    // reset via finalize) and the app stays on the login page (not signed in).
    await expect(signIn).toBeEnabled({ timeout: 10_000 });
    await expect(page).toHaveURL(/\/login/);
  });
});
