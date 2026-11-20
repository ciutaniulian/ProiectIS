import { Client } from '@stomp/stompjs'

export class ChatHttpError extends Error {
  constructor(status) {
    super('Could not load chat history. Try again.')
    this.status = status
  }
}

export async function fetchGroupMessages(groupId, { before, after, limit = 30, signal } = {}) {
  const params = new URLSearchParams({ limit })
  if (before != null) params.set('before', before)
  if (after != null) params.set('after', after)
  const timeout = AbortSignal.timeout(10000)
  const response = await fetch(`/api/group/${groupId}/messages?${params}`, {
    signal: signal ? AbortSignal.any([signal, timeout]) : timeout,
  })
  if (!response.ok) throw new ChatHttpError(response.status)
  return response.json()
}

export function mergeMessages(existing, incoming) {
  const byId = new Map(existing.map(message => [message.messageId, message]))
  for (const message of incoming) byId.set(message.messageId, message)
  return [...byId.values()].sort((left, right) => left.messageId - right.messageId)
}

export async function loadMissedMessages(groupId, after, signal, onPage) {
  let hasMore
  do {
    const page = await fetchGroupMessages(groupId, { after, signal })
    onPage(page.messages)
    after = page.messages.at(-1)?.messageId ?? after
    hasMore = page.hasMore
  } while (hasMore && !signal.aborted)
  return after
}

export function formatMessageTime(isoDateStr) {
  if (!isoDateStr) return ''
  const date = new Date(isoDateStr)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

export function connectGroupChat({ groupId, onMessage, onReady, onStatusChange, onAccessLost, onGroupStateChanged }) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const client = new Client({
    brokerURL: `${protocol}//${window.location.host}/ws`,
    reconnectDelay: 1500,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  })
  let stopped = false
  let ready = false
  let readyTimer
  let pending

  function settleSend(error, message) {
    if (!pending) return
    const { resolve, reject, timer } = pending
    pending = undefined
    clearTimeout(timer)
    if (error) reject(error)
    else resolve(message)
  }

  function disconnect() {
    stopped = true
    ready = false
    clearTimeout(readyTimer)
    settleSend(new Error('The chat connection closed before your message was confirmed.'))
    void client.deactivate()
  }

  function loseAccess() {
    if (stopped) return
    disconnect()
    onAccessLost?.()
  }

  function handleReply({ groupId: replyGroupId, requestId, result, status, message }) {
    if (stopped || (replyGroupId != null && replyGroupId !== groupId)) return
    if (result === 'ACCESS_LOST') {
      loseAccess()
      return
    }
    if (result === 'READY' && !ready) {
      clearTimeout(readyTimer)
      ready = true
      onGroupStateChanged?.(status)
      onStatusChange?.('connected')
      onReady?.()
      return
    }
    if (!pending || (requestId != null && requestId !== pending.requestId)) return
    if (result === 'SENT') {
      settleSend(null, message)
    } else if (result === 'ARCHIVED') {
      onGroupStateChanged?.(status)
      settleSend(new Error('This chat is read-only while the group is archived.'))
    } else if (result === 'FAILED') {
      settleSend(new Error('Could not send your message. Try again.'))
    }
  }

  client.onConnect = () => {
    if (stopped) return
    client.subscribe('/user/queue/chat/replies', frame => handleReply(JSON.parse(frame.body)))
    client.subscribe(`/topic/group/${groupId}`, frame => {
      if (stopped) return
      const payload = JSON.parse(frame.body)
      if (payload.status) onGroupStateChanged?.(payload.status)
      else onMessage?.(payload)
    })
    readyTimer = setTimeout(() => client.forceDisconnect(), 10000)
    client.publish({ destination: `/app/chat/${groupId}/ready` })
  }
  client.onStompError = () => {
    if (!stopped) onStatusChange?.('error')
  }
  client.onWebSocketClose = event => {
    if (event.code === 4003) {
      loseAccess()
      return
    }
    ready = false
    clearTimeout(readyTimer)
    settleSend(new Error('The connection dropped before your message was confirmed. Try again.'))
    if (!stopped) onStatusChange?.('reconnecting')
  }

  onStatusChange?.('connecting')
  client.activate()

  return {
    sendMessage(message, requestId) {
      if (!ready || !client.connected) return Promise.reject(new Error('Chat is reconnecting. Try again shortly.'))
      if (pending) return Promise.reject(new Error('Your previous message is still being sent.'))
      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => settleSend(new Error('Could not confirm your message. Try again.')), 10000)
        pending = { requestId, resolve, reject, timer }
        try {
          client.publish({
            destination: `/app/chat/${groupId}`,
            body: JSON.stringify({ message, requestId }),
          })
        } catch {
          settleSend(new Error('Could not send your message. Try again.'))
        }
      })
    },
    disconnect,
  }
}
