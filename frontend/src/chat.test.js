import assert from 'node:assert/strict'
import { test } from 'node:test'
import { formatMessageTime } from './chat.js'

test('formatMessageTime formats valid ISO date to time string', () => {
  const formatted = formatMessageTime('2026-10-22T14:30:00')
  assert.match(formatted, /\d{1,2}:\d{2}/)
})

test('formatMessageTime handles null or empty input gracefully', () => {
  assert.equal(formatMessageTime(null), '')
  assert.equal(formatMessageTime(''), '')
  assert.equal(formatMessageTime(undefined), '')
})
