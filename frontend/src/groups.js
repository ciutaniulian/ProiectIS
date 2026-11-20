import { getCsrfHeaders } from './auth.js'

const messages = {
  GROUP_FORBIDDEN: 'You no longer have access to this group.',
  GROUP_NOT_ACTIVE: 'This group is archived. Restore it before making changes.',
  GROUP_NOT_ARCHIVED: 'This group is no longer archived.',
  LAST_MEMBER_CONFIRMATION_REQUIRED: 'You are the last member. Confirm that you want to close the group.',
  MEMBER_ALREADY_JOINED: 'This person is already a member of the group.',
  INVITATION_ALREADY_PENDING: 'There is already a pending invitation for this email.',
  INVITATION_NOT_PENDING: 'This invitation is no longer pending.',
  INVITATION_EXPIRED: 'This invitation has expired.',
  INVITATION_NOT_FOUND: 'This invitation is unavailable for your account.',
}

async function request(path, { method = 'GET', data, signal } = {}) {
  let response
  try {
    const headers = method === 'GET' ? {} : await getCsrfHeaders()
    if (data) headers['Content-Type'] = 'application/json'
    response = await fetch(`/api${path}`, {
      method, headers, credentials: 'same-origin', body: data ? JSON.stringify(data) : undefined,
      signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(15000)]) : AbortSignal.timeout(15000),
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new Error('Cannot reach the server. Check your connection and try again.')
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}))
    const fallback = {
      400: 'Check the information you entered and try again.',
      401: 'Your session has ended. Log in and try again.',
      403: 'You do not have access to this resource.',
      404: 'This item is unavailable.',
      409: 'The group has changed. Refresh and try again.',
    }
    throw Object.assign(new Error(messages[problem.code] ?? fallback[response.status]
      ?? 'The request could not be completed. Please try again.'), { status: response.status, code: problem.code })
  }
  return response.status === 204 ? undefined : response.json()
}

const groupPath = (id) => `/groups/${id}`
const invitationsPath = '/group-invitations'

export const listGroups = signal => request('/groups', { signal })
export const createGroup = data => request('/groups', { method: 'POST', data })
export const getGroup = (id, signal) => request(groupPath(id), { signal })
export const updateGroup = (id, data) => request(groupPath(id), { method: 'PATCH', data })
export const removeGroupMember = (id, userId) => request(`${groupPath(id)}/members/${userId}`, { method: 'DELETE' })
export const leaveGroup = (id, confirmClose) => request(`${groupPath(id)}/leave`, { method: 'POST', data: { confirmClose } })
export const archiveGroup = id => request(`${groupPath(id)}/archive`, { method: 'POST' })
export const restoreGroup = id => request(`${groupPath(id)}/restore`, { method: 'POST' })
export const getGroupActivity = (id, page, signal) => request(`${groupPath(id)}/activity?page=${page}&size=20`, { signal })
export const inviteGroupMember = (id, email) => request(`${groupPath(id)}/invitations`, { method: 'POST', data: { email } })
export const revokeGroupInvitation = (id, invitationId) => request(`${groupPath(id)}/invitations/${invitationId}`, { method: 'DELETE' })
export const listGroupInvitations = signal => request(invitationsPath, { signal })
export const getGroupInvitation = (token, signal) => request(`${invitationsPath}/link/${encodeURIComponent(token)}`, { signal })
export const acceptGroupInvitation = id => request(`${invitationsPath}/${id}/accept`, { method: 'POST' })
export const declineGroupInvitation = id => request(`${invitationsPath}/${id}/decline`, { method: 'POST' })

export function memberCountText(count) {
  return `${count} ${count === 1 ? 'member' : 'members'}`
}

export function formatGroupDate(value) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
