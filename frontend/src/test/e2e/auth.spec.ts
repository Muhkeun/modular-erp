import { test, expect } from '@playwright/test'

test.describe('Authentication', () => {
  test('shows login page for unauthenticated users', async ({ page }) => {
    await page.goto('/')
    // Should redirect to login or show login form
    await expect(page).toHaveURL(/\/login/)
  })

  test('can log in with valid credentials', async ({ page }) => {
    await page.goto('/login')

    await page.getByPlaceholder(/email|username|id/i).fill('admin')
    await page.getByPlaceholder(/password/i).fill('admin')
    await page.getByRole('button', { name: /login|sign in|log in/i }).click()

    // After login, should navigate away from login page
    await expect(page).not.toHaveURL(/\/login/, { timeout: 10000 })
  })

  test('shows error for invalid credentials', async ({ page }) => {
    await page.goto('/login')

    await page.getByPlaceholder(/email|username|id/i).fill('invalid@test.com')
    await page.getByPlaceholder(/password/i).fill('wrongpassword')
    await page.getByRole('button', { name: /login|sign in|log in/i }).click()

    // Should remain on login page and show error
    await expect(page).toHaveURL(/\/login/)
    await expect(page.getByText(/error|invalid|failed|incorrect/i)).toBeVisible({ timeout: 5000 })
  })
})
