import test from 'node:test'
import assert from 'node:assert/strict'
import { getProfile, login, logout, register } from './auth.js'

const csrf = (token) => Response.json({ headerName: 'X-CSRF-TOKEN', token })

function stubFetch(t, responses) {
  const calls = []
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    calls.push({ url, ...options })
    const response = responses.shift()
    assert.ok(response, `Unexpected request: ${url}`)
    return response
  })
  return calls
}

test('registration sends JSON with a session cookie and CSRF header', async (t) => {
  const calls = stubFetch(t, [csrf('registration-token'), Response.json({ id: 1 }, { status: 201 })])
  await register({ name: ' Ana ', email: ' ana@example.com ', password: 'secret123' })
  assert.deepEqual(calls.map((call) => call.url), ['/api/auth/csrf', '/api/auth/register'])
  assert.equal(calls[1].headers['Content-Type'], 'application/json')
  assert.equal(calls[1].headers['X-CSRF-TOKEN'], 'registration-token')
  assert.deepEqual(JSON.parse(calls[1].body), { name: 'Ana', email: 'ana@example.com', password: 'secret123' })
  assert.ok(calls.every((call) => call.credentials === 'same-origin'))
})

test('login uses Spring form fields and loads the authenticated profile', async (t) => {
  const profile = { id: 1, name: 'Ana', email: 'ana@example.com', role: 'USER' }
  const calls = stubFetch(t, [csrf('before-login'), new Response(null, { status: 200 }), Response.json(profile)])
  assert.deepEqual(await login({ email: 'ana@example.com', password: 'a&b+c=d', rememberMe: true }), profile)
  assert.equal(calls[1].url, '/api/auth/login')
  assert.equal(calls[1].headers['Content-Type'], 'application/x-www-form-urlencoded')
  assert.equal(calls[1].body.get('username'), profile.email)
  assert.equal(calls[1].body.get('password'), 'a&b+c=d')
  assert.equal(calls[1].body.get('remember-me'), 'true')
  assert.equal(calls[2].url, '/api/auth/me')
})

test('unchecked remember me does not request a persistent cookie', async (t) => {
  const calls = stubFetch(t, [csrf('token'), new Response(), Response.json({ name: 'Ana' })])
  await login({ email: 'ana@example.com', password: 'secret123', rememberMe: false })
  assert.equal(calls[1].body.has('remember-me'), false)
})

test('logout obtains a new CSRF token after login', async (t) => {
  const calls = stubFetch(t, [csrf('before-login'), new Response(), Response.json({ name: 'Ana' }), csrf('after-login'), new Response(null, { status: 204 })])
  await login({ email: 'ana@example.com', password: 'secret123' })
  await logout()
  assert.equal(calls[4].url, '/api/auth/logout')
  assert.equal(calls[4].method, 'POST')
  assert.equal(calls[4].headers['X-CSRF-TOKEN'], 'after-login')
})

test('an anonymous profile is distinct from an unavailable backend', async (t) => {
  stubFetch(t, [new Response(null, { status: 401 }), new Response(null, { status: 503 })])
  assert.equal(await getProfile(), null)
  await assert.rejects(getProfile(), /server could not complete/)
})

test('authentication and validation errors have actionable messages', async (t) => {
  stubFetch(t, [csrf('one'), new Response(null, { status: 401 }), csrf('two'), new Response(null, { status: 409 }), csrf('three'), new Response(null, { status: 403 }), csrf('four'), new Response(null, { status: 400 })])
  await assert.rejects(login({ email: 'ana@example.com', password: 'wrong' }), /Incorrect email or password/)
  await assert.rejects(register({ name: 'Ana', email: 'ana@example.com', password: 'secret123' }), /already registered/)
  await assert.rejects(logout(), /security token has expired/)
  await assert.rejects(register({ name: 'Ana', email: 'invalid', password: 'x' }), /Check your details/)
})

test('network errors do not expose server details', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => { throw new TypeError('internal connection detail') })
  await assert.rejects(getProfile(), { message: 'Cannot reach the server. Check your connection and try again.' })
})

test('a registration 401 is not misreported as an incorrect password', async (t) => {
  stubFetch(t, [csrf('token'), new Response(null, { status: 401 })])
  await assert.rejects(register({ name: 'Ana', email: 'ana@example.com', password: 'secret123' }), { message: 'Your request could not be completed. Please try again.' })
})
