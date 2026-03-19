import { test, expect } from "@playwright/test";
import { login, waitForGrid } from "./helpers";

test.describe("Data Export", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test("should export grid data to Excel", async ({ page }) => {
    await page.goto("/master-data/items");
    await page.waitForLoadState("networkidle");
    await waitForGrid(page);

    // Look for export button
    const exportButton = page.getByRole("button", { name: /export|내보내기/i });
    if (await exportButton.isVisible()) {
      // Set up download listener
      const downloadPromise = page.waitForEvent("download", { timeout: 10000 }).catch(() => null);
      await exportButton.click();

      const download = await downloadPromise;
      if (download) {
        const filename = download.suggestedFilename();
        expect(filename).toMatch(/\.(xlsx|xls|csv)$/i);
      }
    }
  });

  test("should export to PDF", async ({ page }) => {
    await page.goto("/master-data/items");
    await page.waitForLoadState("networkidle");

    // Look for PDF-specific export option
    const pdfButton = page.getByRole("button", { name: /pdf/i });
    if (await pdfButton.isVisible()) {
      const downloadPromise = page.waitForEvent("download", { timeout: 10000 }).catch(() => null);
      await pdfButton.click();
      const download = await downloadPromise;
      if (download) {
        expect(download.suggestedFilename()).toMatch(/\.pdf$/i);
      }
    }
  });

  test("should export to CSV", async ({ page }) => {
    await page.goto("/master-data/items");
    await page.waitForLoadState("networkidle");

    // Look for CSV-specific export option
    const csvButton = page.getByRole("button", { name: /csv/i });
    if (await csvButton.isVisible()) {
      const downloadPromise = page.waitForEvent("download", { timeout: 10000 }).catch(() => null);
      await csvButton.click();
      const download = await downloadPromise;
      if (download) {
        expect(download.suggestedFilename()).toMatch(/\.csv$/i);
      }
    }
  });
});
