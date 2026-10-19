import assert from 'node:assert/strict'
import { test } from 'node:test'
import { demoDashboard, formatMoney } from './dashboard.js'

test('money preserves cents, negative amounts, and zero', () => {
  assert.equal(formatMoney(123456), '1.234,56\u00a0RON')
  assert.equal(formatMoney(-18001), '-180,01\u00a0RON')
  assert.equal(formatMoney(0), '0,00\u00a0RON')
})

test('sample expense total matches the amounts displayed', () => {
  assert.equal(
    demoDashboard.expenses.reduce((total, expense) => total + expense.amountCents, 0),
    demoDashboard.summary.totalExpensesCents,
  )
  assert.equal(new Set(demoDashboard.expenses.map((expense) => expense.id)).size, demoDashboard.expenses.length)
})
