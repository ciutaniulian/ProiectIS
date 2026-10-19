async function request(path, options = {}) {
  let response
  try {
    response = await fetch(`/api/auth/${path}`, {
      credentials: 'same-origin',
      ...options,
      signal: options.signal ? AbortSignal.any([options.signal, AbortSignal.timeout(15000)]) : AbortSignal.timeout(15000),
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new Error('Cannot reach the server. Check your connection and try again.')
  }

  if (response.ok) return response
  if (response.status === 401) {
    if (path === 'me') return null
    if (path === 'login') throw new Error('Incorrect email or password. Please try again.')
    throw new Error('Your request could not be completed. Please try again.')
  }
  if (response.status === 409) throw new Error('This email is already registered. Log in instead.')
  if (response.status === 403) throw new Error('Your security token has expired. Please try again.')
  if (response.status === 400) throw new Error('Check your details. Passwords must contain 6–64 characters.')
  throw new Error('The server could not complete your request. Please try again.')
}

async function submit(path, body, contentType) {
  // Fetch a fresh token because Spring rotates it after login and logout.
  const csrfResponse = await request('csrf')
  const csrf = await csrfResponse.json()
  return request(path, {
    method: 'POST',
    headers: { [csrf.headerName]: csrf.token, ...(contentType && { 'Content-Type': contentType }) },
    body,
  })
}

export async function getProfile(signal) {
  const response = await request('me', signal ? { signal } : {})
  return response ? response.json() : null
}

export async function register({ name, email, password }) {
  await submit('register', JSON.stringify({ name: name.trim(), email: email.trim(), password }), 'application/json')
}

export async function login({ email, password, rememberMe }) {
  const body = new URLSearchParams({ username: email.trim(), password })
  if (rememberMe) body.set('remember-me', 'true')
  await submit('login', body, 'application/x-www-form-urlencoded')
  const profile = await getProfile()
  if (!profile) throw new Error('Your session could not be opened. Please log in again.')
  return profile
}

export async function logout() {
  await submit('logout')
}
