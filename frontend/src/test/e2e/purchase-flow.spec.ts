import { test, expect } from '@playwright/test'

test.describe('Purchase Flow: PR to PO', () => {
  test.beforeEach(async ({ page }) => {
    // Login first
    await page.goto('/login')
    await page.getByPlaceholder(/email|username|id/i).fill('admin')
    await page.getByPlaceholder(/password/i).fill('admin')
    await page.getByRole('button', { name: /login|sign in|log in/i }).click()
    await expect(page).not.toHaveURL(/\/login/, { timeout: 10000 })
  })

  test('create a purchase request', async ({ page }) => {
    await page.goto('/purchase/requests')

    // Click create button
    await page.getByRole('button', { name: /create|new|add/i }).click()

    // Fill in the form
    await page.getByLabel(/description|title/i).first().fill('Test PR from E2E')
    await page.getByLabel(/date|request date/i).first().fill('2026-03-19')

    // Submit
    await page.getByRole('button', { name: /save|submit|create/i }).click()

    // Verify PR was created - look for success indicator or the new entry
    await expect(page.getByText(/success|created|saved/i).or(page.getByText('Test PR from E2E'))).toBeVisible({ timeout: 10000 })
  })

  test('navigate from PR list to PO creation', async ({ page }) => {
    await page.goto('/purchase/requests')

    // Verify we're on the PR page
    await expect(page.getByRole('heading', { name: /purchase request/i })).toBeVisible()

    // Navigate to PO page
    await page.goto('/purchase/orders')
    await expect(page.getByRole('heading', { name: /purchase order/i })).toBeVisible()

    // Click create
    await page.getByRole('button', { name: /create|new|add/i }).click()

    // Should show PO creation form
    await expect(page.getByLabel(/supplier|vendor/i).or(page.getByText(/supplier|vendor/i))).toBeVisible({ timeout: 5000 })
  })
})
