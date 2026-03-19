import { type Page, expect } from "@playwright/test";

/**
 * Login helper — fills the login form and waits for dashboard.
 * The app uses zustand + localStorage for auth, with fields: tenant, loginId, password.
 */
export async function login(
  page: Page,
  loginId = "admin",
  password = "admin123",
  tenantId = "DEFAULT"
) {
  await page.goto("/");
  // Wait for login page to render
  await page.waitForSelector('[data-testid="login-button"]', { timeout: 10000 });

  const tenantInput = page.getByTestId("tenant");
  await tenantInput.clear();
  await tenantInput.fill(tenantId);

  await page.getByTestId("login-id").fill(loginId);
  await page.getByTestId("password").fill(password);
  await page.getByTestId("login-button").click();

  // Wait for navigation to dashboard (auth sets localStorage and redirects)
  await page.waitForURL("**/dashboard", { timeout: 15000 });
}

/**
 * Navigate to a page via sidebar by clicking menu items with matching text.
 * For nested menus, first clicks the parent group to expand it, then the child.
 */
export async function navigateTo(page: Page, ...menuTexts: string[]) {
  const sidebar = page.getByTestId("sidebar-nav");
  for (const text of menuTexts) {
    await sidebar.getByText(text, { exact: false }).first().click();
  }
  // Allow page to load
  await page.waitForLoadState("networkidle");
}

/**
 * Wait for the data grid to be visible and optionally have rows.
 */
export async function waitForGrid(page: Page) {
  await page.getByTestId("data-grid").waitFor({ state: "visible", timeout: 10000 });
}

/**
 * Generate a unique identifier for test data to avoid collisions.
 */
export function uniqueId(prefix = "TEST") {
  return `${prefix}-${Date.now().toString(36).toUpperCase()}`;
}
