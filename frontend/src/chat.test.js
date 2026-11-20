import assert from 'node:assert/strict'
import { test } from 'node:test'
import { formatMessageTime, loadMissedMessages, mergeMessages } from './chat.js'

test('formatMessageTime formats valid ISO date to time string', () => {
  const formatted = formatMessageTime('2026-10-22T14:30:00')
  assert.match(formatted, /\d{1,2}:\d{2}/)
})

test('formatMessageTime handles null or empty input gracefully', () => {
  assert.equal(formatMessageTime(null), '')
  assert.equal(formatMessageTime(''), '')
  assert.equal(formatMessageTime(undefined), '')
})

test('merges history pages and live events in message order without repeating messages', () => {
  const existing = [{ messageId: 4 }, { messageId: 5 }]
  const incoming = [{ messageId: 5 }, { messageId: 3 }, { messageId: 8 }]
  assert.deepEqual(mergeMessages(existing, incoming).map(message => message.messageId), [3, 4, 5, 8])
})

test('loads every missed page, advancing from the last message in each page', async context => {
  const urls = []
  context.mock.method(globalThis, 'fetch', async url => {
    urls.push(url)
    return Response.json(urls.length === 1
      ? { messages: [{ messageId: 20 }, { messageId: 25 }], hasMore: true }
      : { messages: [{ messageId: 40 }], hasMore: false })
  })
  const ids = []
  const latest = await loadMissedMessages(1, 8, new AbortController().signal, messages => {
    ids.push(...messages.map(message => message.messageId))
  })

  assert.equal(latest, 40)
  assert.deepEqual(ids, [20, 25, 40])
  assert.deepEqual(urls, ['/api/group/1/messages?limit=30&after=8', '/api/group/1/messages?limit=30&after=25'])
})
