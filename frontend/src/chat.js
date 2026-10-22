/**
 * Chat API and WebSocket STOMP client manager
 */
import { Client } from '@stomp/stompjs'

/**
 * Fetch historical messages for a group with pagination.
 */
export async function fetchGroupMessages(groupId, page = 0, size = 30, signal) {
  try {
    const response = await fetch(`/api/group/${groupId}/messages?page=${page}&size=${size}`, {
      credentials: 'same-origin',
      signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(10000)]) : AbortSignal.timeout(10000),
    })
    if (!response.ok) {
      throw new Error(`Failed to load messages (${response.status})`)
    }
    const data = await response.json()
    // Backend returns Page<ChatMessageResponse> where messages are in content.
    // In backend, query is OrderByCreatedAtDesc, so the first element is the newest.
    // For chat view, we reverse to display chronologically (oldest to newest).
    const list = Array.isArray(data.content) ? [...data.content].reverse() : []
    return {
      messages: list,
      totalPages: data.totalPages ?? 1,
      totalElements: data.totalElements ?? list.length,
      currentPage: data.number ?? page,
    }
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new Error('Could not load chat history. Check your connection.')
  }
}

/**
 * Formats a message timestamp into a clean hour:minute string.
 */
export function formatMessageTime(isoDateStr) {
  if (!isoDateStr) return ''
  try {
    const date = new Date(isoDateStr)
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  } catch {
    return ''
  }
}

/**
 * Initializes a STOMP over WebSocket connection for a specific group.
 * Returns an object with `sendMessage(text)` and `disconnect()`.
 */
export function connectGroupChat({ groupId, onMessage, onStatusChange }) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const brokerURL = `${protocol}//${window.location.host}/ws`

  const client = new Client({
    brokerURL,
    reconnectDelay: 4000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    debug: () => {},
  })

  client.onConnect = () => {
    onStatusChange?.('connected')
    client.subscribe(`/topic/group/${groupId}`, (frame) => {
      try {
        const payload = JSON.parse(frame.body)
        onMessage?.(payload)
      } catch (err) {
        console.error('Failed to parse incoming chat message frame', err)
      }
    })
  }

  client.onStompError = (frame) => {
    console.error('STOMP error:', frame)
    onStatusChange?.('error')
  }

  client.onWebSocketClose = () => {
    onStatusChange?.('disconnected')
  }

  onStatusChange?.('connecting')
  client.activate()

  return {
    sendMessage: (messageText) => {
      const text = messageText.trim()
      if (!text) return false
      if (!client.connected) {
        throw new Error('Chat is reconnecting. Please wait a moment.')
      }
      client.publish({
        destination: `/app/chat/${groupId}`,
        body: JSON.stringify({ message: text }),
      })
      return true
    },
    disconnect: () => {
      client.deactivate()
      onStatusChange?.('disconnected')
    },
  }
}
