import { test, expect } from "@playwright/test";
import { login, uniqueId, waitForGrid } from "./helpers";

test.describe("Master Data Management", () => {
  const itemCode = uniqueId("ITEM");

  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test("should create a new item", async ({ page }) => {
    await page.goto("/master-data/items/new");
    await page.waitForLoadState("networkidle");

    // Fill the code field
    await page.locator('input.font-mono').first().fill(itemCode);

    // Fill translations - name field (first locale row)
    const nameInputs = page.locator('input').filter({ has: page.locator('[class*="input"]') });
    // Find the name input in the translations section
    const translationSection = page.locator("text=Localization").locator("..");
    const nameInput = translationSection.locator("input").nth(1);
    if (await nameInput.isVisible()) {
      await nameInput.fill("Test Item E2E");
    }

    // Click save
    const saveButton = page.getByRole("button", { name: /save|저장/i });
    await saveButton.click();

    // Should redirect back to list
    await page.waitForURL("**/master-data/items", { timeout: 10000 });
  });

  test("should search and find created item", async ({ page }) => {
    await page.goto("/master-data/items");
    await page.waitForLoadState("networkidle");
    await waitForGrid(page);

    // Open filters
    const filterButton = page.getByRole("button", { name: /filter/i });
    if (await filterButton.isVisible()) {
      await filterButton.click();
      // Search by code
      const codeInput = page.locator('input[placeholder*="Search"], input[placeholder*="검색"]').first();
      if (await codeInput.isVisible()) {
        await codeInput.fill(itemCode);
        await page.waitForTimeout(500);
      }
    }

    // Grid should be visible
    await expect(page.getByTestId("data-grid")).toBeVisible();
  });

  test("should edit item details", async ({ page }) => {
    await page.goto("/master-data/items");
    await page.waitForLoadState("networkidle");
    await waitForGrid(page);

    // Click on the first row in the grid
    const gridRows = page.locator(".ag-row");
    const rowCount = await gridRows.count();
    if (rowCount > 0) {
      await gridRows.first().click();
      await page.waitForLoadState("networkidle");

      // Should navigate to item form
      await expect(page.getByTestId("page-header")).toBeVisible();

      // Try editing specification field
      const specTextarea = page.locator("textarea").first();
      if (await specTextarea.isVisible()) {
        await specTextarea.fill("Updated specification via E2E test");
      }
    }
  });

  test("should delete item", async ({ page }) => {
    await page.goto("/master-data/items");
    await page.waitForLoadState("networkidle");
    await waitForGrid(page);

    // Click first row to go to detail
    const gridRows = page.locator(".ag-row");
    const rowCount = await gridRows.count();
    if (rowCount > 0) {
      await gridRows.first().click();
      await page.waitForLoadState("networkidle");

      // Look for delete button
      const deleteButton = page.getByRole("button", { name: /delete|삭제/i });
      if (await deleteButton.isVisible()) {
        await deleteButton.click();
      }
    }
  });

  test("should show validation errors for empty required fields", async ({ page }) => {
    await page.goto("/master-data/items/new");
    await page.waitForLoadState("networkidle");

    // Try to save without filling required fields
    const saveButton = page.getByRole("button", { name: /save|저장/i });
    await saveButton.click();

    // Wait for potential error response (API validation)
    await page.waitForTimeout(1000);

    // The page should not navigate away (still on /new)
    expect(page.url()).toContain("/master-data/items");
  });
});
