import { test, expect } from "@playwright/test";
import { login } from "./helpers";

test.describe("Authentication Flow", () => {
  test("should show login page when not authenticated", async ({ page }) => {
    await page.goto("/");
    // Should see login form elements
    await expect(page.getByTestId("login-button")).toBeVisible();
    await expect(page.getByTestId("login-id")).toBeVisible();
    await expect(page.getByTestId("password")).toBeVisible();
    await expect(page.getByTestId("tenant")).toBeVisible();
  });

  test("should login with valid credentials", async ({ page }) => {
    await login(page);
    // After login, should be on dashboard
    await expect(page).toHaveURL(/\/dashboard/);
    // Sidebar should be visible
    await expect(page.getByTestId("sidebar")).toBeVisible();
  });

  test("should show dashboard after login", async ({ page }) => {
    await login(page);
    // Page header with dashboard title should be present
    await expect(page.getByTestId("page-header")).toBeVisible();
  });

  test("should persist session on page reload", async ({ page }) => {
    await login(page);
    // Reload the page
    await page.reload();
    await page.waitForLoadState("networkidle");
    // Should still be on dashboard (not redirected to login)
    await expect(page.getByTestId("sidebar")).toBeVisible();
  });

  test("should logout and redirect to login", async ({ page }) => {
    await login(page);
    // Click the logout button
    await page.getByTestId("logout-button").click();
    // Should see login form again
    await expect(page.getByTestId("login-button")).toBeVisible({ timeout: 5000 });
  });

  test("should show error for invalid credentials", async ({ page }) => {
    await page.goto("/");
    await page.waitForSelector('[data-testid="login-button"]');

    await page.getByTestId("login-id").fill("nonexistent@user.com");
    await page.getByTestId("password").fill("wrongpassword");
    await page.getByTestId("login-button").click();

    // Should show error message
    await expect(page.getByTestId("login-error")).toBeVisible({ timeout: 10000 });
  });
});
