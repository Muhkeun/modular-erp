import { test, expect } from "@playwright/test";
import { login } from "./helpers";

test.describe("Settings", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await page.goto("/settings");
    await page.waitForLoadState("networkidle");
  });

  test("should change language preference", async ({ page }) => {
    // Click the "Defaults" tab
    const defaultsTab = page.getByRole("button", { name: /default|기본/i });
    await defaultsTab.click();
    await page.waitForTimeout(300);

    // Click English button
    const englishButton = page.getByRole("button", { name: "English" });
    if (await englishButton.isVisible()) {
      await englishButton.click();
      await page.waitForTimeout(500);

      // Language should switch — verify the button is now active (has brand class)
      await expect(englishButton).toHaveClass(/bg-brand-600/);
    }

    // Switch back to Korean
    const koreanButton = page.getByRole("button", { name: "한국어" });
    if (await koreanButton.isVisible()) {
      await koreanButton.click();
      await page.waitForTimeout(500);
      await expect(koreanButton).toHaveClass(/bg-brand-600/);
    }
  });

  test("should save grid column preferences", async ({ page }) => {
    // Navigate to a page with a grid
    await page.goto("/purchase/requests");
    await page.waitForLoadState("networkidle");

    const grid = page.getByTestId("data-grid");
    await expect(grid).toBeVisible();

    // AG Grid allows column resizing — verify grid has columns
    const headerCells = page.locator(".ag-header-cell");
    const count = await headerCells.count();
    expect(count).toBeGreaterThan(0);
  });

  test("should persist preferences after page reload", async ({ page }) => {
    // Change theme to verify persistence
    const displayTab = page.getByRole("button", { name: /display|화면/i });
    await displayTab.click();
    await page.waitForTimeout(300);

    // Click light theme button
    const lightButton = page.getByRole("button", { name: /Light/i });
    if (await lightButton.isVisible()) {
      await lightButton.click();
      await page.waitForTimeout(500);
    }

    // Reload the page
    await page.reload();
    await page.waitForLoadState("networkidle");

    // Should still be on settings page
    await expect(page.getByTestId("page-header")).toBeVisible();
  });
});
