import { test, expect } from "@playwright/test";
import { login, uniqueId } from "./helpers";

test.describe("CRM Pipeline", () => {
  const customerName = `Customer ${uniqueId()}`;
  const leadName = `Lead ${uniqueId()}`;

  test.beforeEach(async ({ page }) => {
    await login(page);
    await page.goto("/crm");
    await page.waitForLoadState("networkidle");
  });

  test("should create a new customer", async ({ page }) => {
    // Click Customers tab (should be active by default)
    const customersTab = page.getByRole("button", { name: /customer/i }).first();
    await customersTab.click();

    // Click New button
    const newButton = page.getByRole("button", { name: /new|신규/i });
    await newButton.click();
    await page.waitForLoadState("networkidle");

    // Fill customer form
    const nameInput = page.locator("input").first();
    await nameInput.fill(customerName);

    // Fill email
    const emailInput = page.locator('input[type="email"]').first();
    if (await emailInput.isVisible()) {
      await emailInput.fill("customer-e2e@test.com");
    }

    // Click save
    const saveButton = page.getByRole("button", { name: /save|저장/i });
    await saveButton.click();

    // Should return to list mode
    await page.waitForLoadState("networkidle");
    await expect(page.getByTestId("data-grid")).toBeVisible({ timeout: 10000 });
  });

  test("should create a lead", async ({ page }) => {
    // Click Leads tab
    const leadsTab = page.getByRole("button", { name: /lead/i }).first();
    await leadsTab.click();
    await page.waitForLoadState("networkidle");

    // Click New button
    const newButton = page.getByRole("button", { name: /new|신규/i });
    await newButton.click();
    await page.waitForLoadState("networkidle");

    // Fill lead form
    const nameInput = page.locator("input").first();
    await nameInput.fill(leadName);

    // Save
    const saveButton = page.getByRole("button", { name: /save|저장/i });
    await saveButton.click();
    await page.waitForLoadState("networkidle");
  });

  test("should convert lead to customer", async ({ page }) => {
    // Click Leads tab
    const leadsTab = page.getByRole("button", { name: /lead/i }).first();
    await leadsTab.click();
    await page.waitForLoadState("networkidle");

    // Click on a lead row — the app uses confirm() dialog for conversion
    const grid = page.getByTestId("data-grid");
    await expect(grid).toBeVisible();

    const rows = page.locator(".ag-row");
    const rowCount = await rows.count();
    if (rowCount > 0) {
      // Set up dialog handler before clicking
      page.on("dialog", async (dialog) => {
        await dialog.accept();
      });
      await rows.first().click();
      await page.waitForTimeout(2000);
    }
  });

  test("should create opportunity and update stages", async ({ page }) => {
    // Click Opportunities tab
    const oppsTab = page.getByRole("button", { name: /opportunit/i }).first();
    await oppsTab.click();
    await page.waitForLoadState("networkidle");

    // Create new opportunity
    const newButton = page.getByRole("button", { name: /new|신규/i });
    await newButton.click();
    await page.waitForLoadState("networkidle");

    // Fill form
    const nameInput = page.locator("input").first();
    await nameInput.fill(`Opportunity ${uniqueId()}`);

    const amountInput = page.locator('input[type="number"]').first();
    if (await amountInput.isVisible()) {
      await amountInput.fill("1000000");
    }

    const saveButton = page.getByRole("button", { name: /save|저장/i });
    await saveButton.click();
    await page.waitForLoadState("networkidle");
  });

  test("should switch between CRM tabs", async ({ page }) => {
    // Customers
    await page.getByRole("button", { name: /customer/i }).first().click();
    await page.waitForTimeout(500);
    await expect(page.getByTestId("data-grid")).toBeVisible();

    // Leads
    await page.getByRole("button", { name: /lead/i }).first().click();
    await page.waitForTimeout(500);
    await expect(page.getByTestId("data-grid")).toBeVisible();

    // Opportunities
    await page.getByRole("button", { name: /opportunit/i }).first().click();
    await page.waitForTimeout(500);
    await expect(page.getByTestId("data-grid")).toBeVisible();

    // Activities
    await page.getByRole("button", { name: /activit/i }).first().click();
    await page.waitForTimeout(500);
    await expect(page.getByTestId("data-grid")).toBeVisible();
  });
});
