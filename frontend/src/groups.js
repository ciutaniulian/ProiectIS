import { getCsrfToken } from './auth.js'

const groupPath = (id) => `/api/groups/${id}`
const errors = {
  400: 'Check the information and try again.',
  401: 'Your session expired. Log in again.',
  403: 'You no longer have permission to perform this action. Refresh and try again.',
  404: 'The group or member could not be found.',
}

async function groupRequest(path, { method = 'GET', body, signal, missing, conflict } = {}) {
  const headers = {}
  if (method !== 'GET') {
    const { headerName, token } = await getCsrfToken()
    headers[headerName] = token
  }
  if (body) headers['Content-Type'] = 'application/json'

  const deadline = AbortSignal.timeout(15000)
  let response
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      credentials: 'same-origin',
      signal: signal ? AbortSignal.any([signal, deadline]) : deadline,
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new Error('Cannot reach the server. Check your connection and try again.')
  }

  if (!response.ok) {
    const message = (response.status === 409 && conflict) || (response.status === 404 && missing)
    throw new Error(message || errors[response.status] || 'The request could not be completed. Try again.')
  }
  const contentType = response.headers.get('content-type')
  return contentType?.includes('application/json') ? response.json() : undefined
}

export const getGroups = (signal) => groupRequest('/api/groups', { signal })
export const createGroup = (data) => groupRequest('/api/groups', { method: 'POST', body: data })
export const getGroupMembers = (groupId, signal) => groupRequest(`${groupPath(groupId)}/members`, { signal })
export const addGroupMember = (groupId, email) => groupRequest(`${groupPath(groupId)}/members`, {
  method: 'POST',
  body: { email },
  missing: 'No account was found for that email, or the group is unavailable.',
  conflict: 'This person is already in the group.',
})
export const promoteGroupMember = (groupId, userId) =>
  groupRequest(`${groupPath(groupId)}/members/${userId}/role`, { method: 'PATCH' })
export const removeGroupMember = (groupId, userId) =>
  groupRequest(`${groupPath(groupId)}/members/${userId}`, { method: 'DELETE' })
export const leaveGroup = (groupId) => groupRequest(`${groupPath(groupId)}/members/me`, {
  method: 'DELETE',
  conflict: 'Promote another member to administrator before leaving.',
})
