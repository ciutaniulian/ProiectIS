import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { connectGroupChat, fetchGroupMessages, formatMessageTime, loadMissedMessages, mergeMessages } from './chat.js'
import './GroupChat.css'

const statusText = {
  connecting: 'Connecting…',
  connected: 'Live chat',
  reconnecting: 'Reconnecting…',
  error: 'Connection issue',
}

function accessDenied(error) {
  return error.status === 401 || error.status === 403 || error.status === 404
}

export default function GroupChat({ group, currentUser, onClose, onAccessLost, onGroupStateChanged }) {
  const [messages, setMessages] = useState([])
  const [olderCursor, setOlderCursor] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadingOlder, setLoadingOlder] = useState(false)
  const [historyError, setHistoryError] = useState(false)
  const [inputText, setInputText] = useState('')
  const [sending, setSending] = useState(false)
  const [socketStatus, setSocketStatus] = useState('connecting')
  const [liveGroupStatus, setLiveGroupStatus] = useState(null)
  const [sendError, setSendError] = useState('')
  const chatClient = useRef(null)
  const callbacks = useRef(null)
  const sendRequest = useRef(null)
  const chatBody = useRef(null)
  const closeButton = useRef(null)
  const dialog = useRef(null)
  const requestedScroll = useRef(null)
  const history = useRef(null)
  const archived = (onGroupStateChanged ? group.status : liveGroupStatus ?? group.status) === 'ARCHIVED'
  const groupId = group.id

  useEffect(() => {
    callbacks.current = { onClose, onAccessLost, onGroupStateChanged }
  }, [onClose, onAccessLost, onGroupStateChanged])

  useEffect(() => {
    const previousFocus = document.activeElement
    closeButton.current.focus()
    return () => previousFocus?.focus()
  }, [])

  useLayoutEffect(() => {
    const body = chatBody.current
    const scroll = requestedScroll.current
    requestedScroll.current = null
    if (scroll === 'bottom') body.scrollTop = body.scrollHeight
    if (scroll && scroll !== 'bottom') {
      body.scrollTop = body.scrollHeight - scroll.height + scroll.top
    }
  }, [messages])

  useEffect(() => {
    const controller = new AbortController()
    const { signal } = controller
    let loadedThrough
    let reading
    let readyVersion = 0
    let syncedVersion = -1
    let connected = false
    let hasLostAccess = false

    function loseAccess() {
      if (hasLostAccess || signal.aborted) return
      hasLostAccess = true
      controller.abort()
      chatClient.current?.disconnect()
      const handler = callbacks.current.onAccessLost ?? callbacks.current.onClose
      handler()
    }

    function display(incoming, forceScroll = false) {
      if (signal.aborted || incoming.length === 0) return
      const body = chatBody.current
      if (forceScroll || (!requestedScroll.current && body.scrollHeight - body.clientHeight - body.scrollTop < 100)) {
        requestedScroll.current = 'bottom'
      }
      setMessages(existing => mergeMessages(existing, incoming))
    }

    async function read() {
      if (reading) {
        await reading
        if (connected && syncedVersion < readyVersion && !signal.aborted) return read()
        return
      }
      setHistoryError(false)
      const version = readyVersion
      reading = (async () => {
        try {
          if (loadedThrough === undefined) {
            const page = await fetchGroupMessages(groupId, { signal })
            if (signal.aborted) return
            loadedThrough = page.messages[0]?.messageId ?? 0
            setOlderCursor(page.hasMore ? page.messages.at(-1).messageId : null)
            display(page.messages, true)
          }
          if (connected) {
            loadedThrough = await loadMissedMessages(groupId, loadedThrough, signal, display)
          }
        } catch (error) {
          if (signal.aborted) return
          if (accessDenied(error)) loseAccess()
          else setHistoryError(true)
        } finally {
          syncedVersion = version
          if (!signal.aborted) setLoading(false)
        }
      })()
      try {
        await reading
      } finally {
        reading = undefined
      }
    }

    history.current = { signal, read, loseAccess }
    const client = connectGroupChat({
      groupId,
      onMessage: message => display([message]),
      onReady: () => {
        connected = true
        readyVersion++
        void read()
      },
      onStatusChange: status => {
        if (signal.aborted) return
        setSocketStatus(status)
        if (status !== 'connected') connected = false
        if (status === 'error') {
          fetchGroupMessages(groupId, { limit: 1, signal }).catch(error => {
            if (accessDenied(error)) loseAccess()
          })
        }
      },
      onGroupStateChanged: status => {
        if (signal.aborted) return
        const handler = callbacks.current.onGroupStateChanged
        if (handler) handler(status)
        else setLiveGroupStatus(status)
      },
      onAccessLost: loseAccess,
    })
    chatClient.current = client
    void read()
    return () => {
      controller.abort()
      client.disconnect()
    }
  }, [groupId])

  async function loadOlder() {
    if (loadingOlder || !olderCursor) return
    setLoadingOlder(true)
    setHistoryError(false)
    const { signal, loseAccess } = history.current
    try {
      const page = await fetchGroupMessages(groupId, { before: olderCursor, signal })
      if (signal.aborted) return
      const body = chatBody.current
      requestedScroll.current = { height: body.scrollHeight, top: body.scrollTop }
      setMessages(existing => mergeMessages(existing, page.messages))
      setOlderCursor(page.hasMore ? page.messages.at(-1).messageId : null)
    } catch (error) {
      if (signal.aborted) return
      if (accessDenied(error)) loseAccess()
      else setHistoryError(true)
    } finally {
      if (!signal.aborted) setLoadingOlder(false)
    }
  }

  async function handleSend(event) {
    event.preventDefault()
    const text = inputText.trim()
    if (!text || sending || archived) return
    setSending(true)
    setSendError('')
    if (sendRequest.current?.text !== text) {
      sendRequest.current = { text, id: crypto.randomUUID() }
    }
    const { signal } = history.current
    try {
      const message = await chatClient.current.sendMessage(text, sendRequest.current.id)
      if (signal.aborted) return
      requestedScroll.current = 'bottom'
      setMessages(existing => mergeMessages(existing, [message]))
      setInputText('')
      sendRequest.current = null
    } catch (error) {
      if (!signal.aborted) setSendError(error.message)
    } finally {
      if (!signal.aborted) setSending(false)
    }
  }

  function handleDialogKeys(event) {
    if (event.key === 'Escape') {
      event.preventDefault()
      onClose()
    }
    if (event.key !== 'Tab') return
    const focusable = dialog.current.querySelectorAll('button:not(:disabled), input:not(:disabled), [tabindex="0"]')
    const first = focusable[0]
    const last = focusable[focusable.length - 1]
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault()
      first.focus()
    }
  }

  return (
    <div className="chat-overlay" role="dialog" aria-modal="true" aria-label={`Chat for ${group.name}`}
      ref={dialog} onKeyDown={handleDialogKeys}>
      <div className="chat-modal">
        <header className="chat-header">
          <button type="button" className="chat-back-btn" onClick={onClose} ref={closeButton} aria-label="Close chat">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
          </button>
          <div className="chat-group-info">
            <span className="chat-group-avatar" aria-hidden="true">{group.name.slice(0, 1)}</span>
            <div className="chat-group-meta">
              <h2>{group.name}</h2>
              <div className="chat-status" role="status">
                <span className={`status-dot ${socketStatus}`} />
                <span>{statusText[socketStatus]}</span>
                <span>· {group.memberCount} {group.memberCount === 1 ? 'member' : 'members'}</span>
              </div>
            </div>
          </div>
        </header>

        <div className="chat-body" ref={chatBody} tabIndex={0} aria-label="Messages">
          {historyError && <div className="chat-error-banner" role="alert">
            Could not load chat history. <button type="button" className="text-button" onClick={() => history.current.read()}>Retry</button>
          </div>}
          {olderCursor && <button type="button" className="text-button chat-load-older" onClick={loadOlder} disabled={loadingOlder}>
            {loadingOlder ? 'Loading earlier messages…' : 'Load older messages'}
          </button>}
          {loading && <p className="chat-empty" role="status">Loading messages…</p>}
          {!loading && !historyError && messages.length === 0 && <div className="chat-empty">
            <p>No messages yet in <strong>{group.name}</strong>.</p>
          </div>}
          <div className="chat-messages" role="log" aria-label="Group messages">
            {messages.map(message => {
              const mine = message.senderId === currentUser?.id
              return (
                <div key={message.messageId} className={`chat-message-row ${mine ? 'mine' : 'theirs'}`}>
                  {!mine && <span className="chat-msg-avatar" aria-hidden="true">{message.senderName?.slice(0, 1) || 'U'}</span>}
                  <div className="chat-bubble">
                    {!mine && <span className="chat-sender-name">{message.senderName}</span>}
                    <span className="chat-text">{message.message}</span>
                    <time className="chat-time" dateTime={message.createdAt}>{formatMessageTime(message.createdAt)}</time>
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        <footer className="chat-footer">
          {sendError && !archived && <p className="chat-error-banner" role="alert">{sendError}</p>}
          {archived ? (
            <p className="chat-readonly" role="status">This chat is read-only while the group is archived.</p>
          ) : (
            <form className="chat-input-form" onSubmit={handleSend}>
              <div className="chat-input-wrap">
                <input type="text" className="chat-input" placeholder="Type a message…" aria-label="Message"
                  value={inputText} maxLength={100} onChange={event => setInputText(event.target.value)}
                  readOnly={sending} disabled={socketStatus !== 'connected'} />
                <span className="chat-char-count">{inputText.length}/100</span>
              </div>
              <button type="submit" className="chat-send-btn" aria-label="Send message"
                disabled={!inputText.trim() || socketStatus !== 'connected' || sending}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <line x1="22" y1="2" x2="11" y2="13" />
                  <polygon points="22 2 15 22 11 13 2 9 22 2" />
                </svg>
              </button>
            </form>
          )}
        </footer>
      </div>
    </div>
  )
}
