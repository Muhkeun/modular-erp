import { test, expect } from "@playwright/test";
import { login, uniqueId, waitForGrid } from "./helpers";

test.describe("End-to-End Business Scenario", () => {
  test("complete procurement cycle via UI", async ({ page }) => {
    test.setTimeout(120000); // 2 minutes for full flow

    const itemCode = uniqueId("BIZ");

    // 1. Login
    await login(page);
    await expect(page).toHaveURL(/\/dashboard/);

    // 2. Create Item in Master Data
    await page.goto("/master-data/items/new");
    await page.waitForLoadState("networkidle");

    const codeInput = page.locator("input.font-mono").first();
    await codeInput.fill(itemCode);

    // Fill item name in translations section
    const translationNameInput = page.locator("section").filter({ hasText: /Localization|다국어/ }).locator("input").nth(1);
    if (await translationNameInput.isVisible()) {
      await translationNameInput.fill("Business Scenario Test Item");
    }

    const saveItemButton = page.getByRole("button", { name: /save|저장/i });
    await saveItemButton.click();
    await page.waitForURL("**/master-data/items", { timeout: 10000 });

    // 3. Navigate to Purchase Request
    await page.goto("/purchase/requests");
    await page.waitForLoadState("networkidle");

    // 4. Create PR with the item
    const newPrButton = page.getByRole("button", { name: /new|신규|구매요청/i });
    await newPrButton.click();
    await page.waitForLoadState("networkidle");

    // Fill delivery date
    const dateInput = page.locator('input[type="date"]');
    if (await dateInput.isVisible()) {
      await dateInput.fill("2026-07-01");
    }

    // Fill description
    const descTextarea = page.locator("textarea").first();
    if (await descTextarea.isVisible()) {
      await descTextarea.fill("E2E Business Scenario Test");
    }

    // Fill quantity
    const qtyInput = page.locator('input[type="number"]').first();
    if (await qtyInput.isVisible()) {
      await qtyInput.fill("20");
    }

    // Fill unit price
    const priceInput = page.locator('input[type="number"]').nth(1);
    if (await priceInput.isVisible()) {
      await priceInput.fill("10000");
    }

    // Save PR
    const savePrButton = page.getByRole("button", { name: /save|저장/i });
    await savePrButton.click();
    await page.waitForURL("**/purchase/requests", { timeout: 10000 });
    await waitForGrid(page);

    // 5. Submit PR — click on the first DRAFT row
    const draftRow = page.locator(".ag-row").filter({ hasText: "DRAFT" }).first();
    if (await draftRow.isVisible()) {
      await draftRow.click();
      await page.waitForLoadState("networkidle");

      const submitButton = page.getByRole("button", { name: /submit|제출/i });
      if (await submitButton.isVisible()) {
        await submitButton.click();
        await page.waitForTimeout(2000);
      }

      // 6. Approve PR
      const approveButton = page.getByRole("button", { name: /approve|승인/i });
      if (await approveButton.isVisible()) {
        await approveButton.click();
        await page.waitForTimeout(2000);
      }
    }

    // 7. Navigate to Purchase Order
    await page.goto("/purchase/orders");
    await page.waitForLoadState("networkidle");

    // 8. Verify PO page loads
    await expect(page.getByTestId("page-header")).toBeVisible();

    // 9. Navigate to Goods Receipt
    await page.goto("/logistics/gr");
    await page.waitForLoadState("networkidle");
    await expect(page.getByTestId("page-header")).toBeVisible();

    // 10. Verify Goods Receipt page
    await expect(page.getByTestId("data-grid")).toBeVisible({ timeout: 10000 });

    // 11. Navigate to Stock Overview
    await page.goto("/logistics/stock");
    await page.waitForLoadState("networkidle");

    // 12. Verify Stock Overview page loads
    await expect(page.getByTestId("page-header")).toBeVisible();

    // 13. Verify stock grid is visible
    await expect(page.getByTestId("data-grid")).toBeVisible({ timeout: 10000 });
  });
});
