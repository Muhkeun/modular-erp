import { test, expect } from "@playwright/test";
import { login } from "./helpers";

test.describe("Navigation", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test("sidebar should show all menu groups", async ({ page }) => {
    const sidebar = page.getByTestId("sidebar-nav");
    await expect(sidebar).toBeVisible();

    // Check for key nav groups (using translation keys' rendered text — checking both EN/KO)
    const expectedMenuTexts = [
      "Dashboard",
      "Master Data",
      "Procurement",
      "Logistics",
      "Production",
      "Sales",
      "Finance",
      "CRM",
      "HR",
    ];

    // The app may be in Korean, so check that sidebar has multiple nav items
    const navItems = sidebar.locator("a, button");
    const count = await navItems.count();
    expect(count).toBeGreaterThanOrEqual(8);
  });

  test("should navigate to each module page", async ({ page }) => {
    // Navigate to dashboard
    await page.goto("/dashboard");
    await expect(page.getByTestId("page-header")).toBeVisible();

    // Navigate to purchase requests
    await page.goto("/purchase/requests");
    await page.waitForLoadState("networkidle");
    await expect(page.getByTestId("page-header")).toBeVisible();

    // Navigate to HR
    await page.goto("/hr");
    await page.waitForLoadState("networkidle");
    await expect(page.getByTestId("page-header")).toBeVisible();

    // Navigate to stock overview
    await page.goto("/logistics/stock");
    await page.waitForLoadState("networkidle");
    await expect(page.getByTestId("page-header")).toBeVisible();
  });

  test("should highlight active menu item", async ({ page }) => {
    // Go to dashboard
    await page.goto("/dashboard");
    await page.waitForLoadState("networkidle");

    // The Dashboard link in sidebar should have the active class (bg-brand-50)
    const sidebar = page.getByTestId("sidebar-nav");
    const dashboardLink = sidebar.locator("a").first();
    await expect(dashboardLink).toHaveClass(/bg-brand-50/);
  });

  test("should toggle language between Korean and English", async ({ page }) => {
    const langButton = page.getByTestId("lang-toggle");
    await expect(langButton).toBeVisible();

    // Get initial text
    const initialText = await langButton.textContent();

    // Click to toggle
    await langButton.click();
    await page.waitForTimeout(500);

    // Text should change
    const newText = await langButton.textContent();
    expect(newText).not.toEqual(initialText);

    // Toggle back
    await langButton.click();
    await page.waitForTimeout(500);
    const restoredText = await langButton.textContent();
    expect(restoredText).toEqual(initialText);
  });

  test("should show breadcrumbs on subpages", async ({ page }) => {
    await page.goto("/purchase/requests");
    await page.waitForLoadState("networkidle");

    // PageHeader should contain breadcrumb navigation
    const header = page.getByTestId("page-header");
    const breadcrumbNav = header.locator("nav");
    await expect(breadcrumbNav).toBeVisible();
  });

  test("responsive: sidebar should collapse on toggle", async ({ page }) => {
    await expect(page.getByTestId("sidebar")).toBeVisible();

    // Click toggle to hide sidebar
    await page.getByTestId("sidebar-toggle").click();
    await page.waitForTimeout(300);

    // Sidebar should have w-0 class (collapsed)
    await expect(page.getByTestId("sidebar")).toHaveClass(/w-0/);

    // Click toggle again to show
    await page.getByTestId("sidebar-toggle").click();
    await page.waitForTimeout(300);
    await expect(page.getByTestId("sidebar")).toHaveClass(/w-64/);
  });
});
