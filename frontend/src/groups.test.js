import assert from 'node:assert/strict'
import test from 'node:test'
import { createGroup, getGroups, leaveGroup, promoteGroupMember, removeGroupMember } from './groups.js'

function stubFetch(t, ...responses) {
  const calls = []
  t.mock.method(globalThis, 'fetch', async (path, options) => {
    calls.push({ path, ...options })
    const response = responses.shift()
    assert.ok(response, `Unexpected request to ${path}`)
    return response
  })
  return calls
}

const csrf = () => Response.json({ headerName: 'X-CSRF-TOKEN', token: 'test-token' })

test('group list preserves membership roles and creation uses the current CSRF token', async (t) => {
  const group = { id: 12, name: 'Trip', role: 'ADMIN' }
  const calls = stubFetch(t, Response.json([group]), csrf(), Response.json(group, { status: 201 }))
  assert.deepEqual(await getGroups(), [group])
  assert.deepEqual(await createGroup({ name: 'Trip' }), group)
  assert.deepEqual(calls.map(({ path }) => path), ['/api/groups', '/api/auth/csrf', '/api/groups'])
  assert.equal(calls[2].headers['X-CSRF-TOKEN'], 'test-token')
  assert.deepEqual(JSON.parse(calls[2].body), { name: 'Trip' })
})

test('promotion, removal and voluntary exit call separate API operations', async (t) => {
  const noContent = () => new Response(null, { status: 204 })
  const calls = stubFetch(t, csrf(), noContent(), csrf(), noContent(), csrf(), noContent())
  await promoteGroupMember(12, 3)
  await removeGroupMember(12, 3)
  await leaveGroup(12)
  assert.deepEqual(calls.filter(({ method }) => method).map(({ method, path }) => [method, path]), [
    ['PATCH', '/api/groups/12/members/3/role'],
    ['DELETE', '/api/groups/12/members/3'],
    ['DELETE', '/api/groups/12/members/me'],
  ])
})

test('the last administrator sees how to fix a blocked exit', async (t) => {
  stubFetch(t, csrf(), new Response(null, { status: 409 }))
  await assert.rejects(leaveGroup(12), /Promote another member/)
})
