import { useEffect, useRef, useState } from 'react'
import { connectGroupChat, fetchGroupMessages, formatMessageTime } from './chat.js'
import './GroupChat.css'

export default function GroupChat({ group, currentUser, onClose }) {
  const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState(true)
  const [inputText, setInputText] = useState('')
  const [socketStatus, setSocketStatus] = useState('connecting')
  const [errorMsg, setErrorMsg] = useState('')

  const chatClientRef = useRef(null)
  const messagesEndRef = useRef(null)

  const scrollToBottom = (smooth = true) => {
    messagesEndRef.current?.scrollIntoView({ behavior: smooth ? 'smooth' : 'auto' })
  }

  // 1. Load historical messages via REST
  useEffect(() => {
    const controller = new AbortController()
    fetchGroupMessages(group.id, 0, 50, controller.signal)
      .then((data) => {
        setMessages(data.messages)
        setTimeout(() => scrollToBottom(false), 50)
      })
      .catch((err) => {
        if (err.name !== 'AbortError') {
          setErrorMsg(err.message)
        }
      })
      .finally(() => {
        setLoading(false)
      })

    return () => controller.abort()
  }, [group.id])

  // 2. Connect to STOMP over WebSocket
  useEffect(() => {
    const client = connectGroupChat({
      groupId: group.id,
      onMessage: (newMsg) => {
        setMessages((prev) => {
          // Avoid duplicate messages if already present
          if (prev.some((m) => m.messageId === newMsg.messageId)) return prev
          return [...prev, newMsg]
        })
        setTimeout(() => scrollToBottom(true), 60)
      },
      onStatusChange: (status) => {
        setSocketStatus(status)
      },
    })

    chatClientRef.current = client

    return () => {
      client.disconnect()
    }
  }, [group.id])

  // 3. Handle escape key
  useEffect(() => {
    const onKeyDown = (e) => {
      if (e.key === 'Escape') onClose?.()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  const handleSend = (e) => {
    e.preventDefault()
    const text = inputText.trim()
    if (!text) return

    try {
      setErrorMsg('')
      const sent = chatClientRef.current?.sendMessage(text)
      if (sent) {
        setInputText('')
      }
    } catch (err) {
      setErrorMsg(err.message)
    }
  }

  const isMyMessage = (msg) => {
    if (!currentUser) return false
    if (msg.senderId && currentUser.id) {
      return Number(msg.senderId) === Number(currentUser.id)
    }
    return msg.senderName === currentUser.name
  }

  return (
    <div className="chat-overlay" role="dialog" aria-modal="true" aria-label={`Chat for ${group.name}`}>
      <div className="chat-modal">
        {/* Header */}
        <header className="chat-header">
          <button
            type="button"
            className="chat-back-btn"
            onClick={onClose}
            aria-label="Close chat"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
          </button>
          <div className="chat-group-info">
            <span className="chat-group-avatar" aria-hidden="true">
              {group.name.slice(0, 1)}
            </span>
            <div className="chat-group-meta">
              <h2>{group.name}</h2>
              <div className="chat-status">
                <span className={`status-dot ${socketStatus}`} />
                <span>
                  {socketStatus === 'connected' && 'Live chat'}
                  {socketStatus === 'connecting' && 'Connecting…'}
                  {socketStatus === 'disconnected' && 'Disconnected'}
                  {socketStatus === 'error' && 'Connection issue'}
                </span>
                <span>· {group.memberCount} members</span>
              </div>
            </div>
          </div>
        </header>

        {/* Messages Body */}
        <div className="chat-body" tabIndex={0} aria-live="polite">
          {errorMsg && <div className="chat-error-banner">{errorMsg}</div>}

          {loading ? (
            <div className="chat-empty">
              <p>Loading messages…</p>
            </div>
          ) : messages.length === 0 ? (
            <div className="chat-empty">
              <div className="chat-empty-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                </svg>
              </div>
              <p>No messages yet in <strong>{group.name}</strong>.</p>
              <span>Say hello to your group!</span>
            </div>
          ) : (
            messages.map((msg, idx) => {
              const mine = isMyMessage(msg)
              return (
                <div key={msg.messageId || idx} className={`chat-message-row ${mine ? 'mine' : 'theirs'}`}>
                  {!mine && (
                    <span className="chat-msg-avatar" aria-hidden="true">
                      {msg.senderName?.slice(0, 1) || 'U'}
                    </span>
                  )}
                  <div className="chat-bubble">
                    {!mine && <span className="chat-sender-name">{msg.senderName}</span>}
                    <span className="chat-text">{msg.message}</span>
                    <time className="chat-time">{formatMessageTime(msg.createdAt)}</time>
                  </div>
                </div>
              )
            })
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Footer / Input */}
        <footer className="chat-footer">
          <form className="chat-input-form" onSubmit={handleSend}>
            <div className="chat-input-wrap">
              <input
                type="text"
                className="chat-input"
                placeholder="Type a message…"
                value={inputText}
                maxLength={100}
                onChange={(e) => setInputText(e.target.value)}
                disabled={socketStatus !== 'connected'}
                autoFocus
              />
              <span className="chat-char-count">{inputText.length}/100</span>
            </div>
            <button
              type="submit"
              className="chat-send-btn"
              disabled={!inputText.trim() || socketStatus !== 'connected'}
              aria-label="Send message"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
                <line x1="22" y1="2" x2="11" y2="13" />
                <polygon points="22 2 15 22 11 13 2 9 22 2" />
              </svg>
            </button>
          </form>
        </footer>
      </div>
    </div>
  )
}
