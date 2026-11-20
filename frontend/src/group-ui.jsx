import { useEffect, useRef } from 'react'

export function GroupLink({ href, navigate, children, ...attributes }) {
  return <a href={href} {...attributes} onClick={event => {
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return
    event.preventDefault()
    navigate(href)
  }}>{children}</a>
}

export function ConfirmGroupAction({ action, busy, error, onCancel, onConfirm }) {
  const dialog = useRef(null)
  useEffect(() => {
    const element = dialog.current
    element.showModal()
    return () => element.close()
  }, [])

  return (
    <dialog ref={dialog} role="alertdialog" className="group-dialog" aria-labelledby="group-confirm-title"
      aria-describedby="group-confirm-description" onCancel={event => {
        event.preventDefault()
        if (!busy) onCancel()
      }}>
      <h2 id="group-confirm-title">{action.label}</h2>
      <p id="group-confirm-description">{action.description}</p>
      {error && <p className="group-form-error" role="alert">{error}</p>}
      <div className="group-form-actions">
        <button type="button" className="text-button" disabled={busy} autoFocus onClick={onCancel}>Cancel</button>
        <button type="button" className="group-create-submit" disabled={busy} onClick={onConfirm}>{action.label}</button>
      </div>
    </dialog>
  )
}

export function GroupFields({ name, description, onName, onDescription, prefix, autoFocus }) {
  return (
    <>
      <label htmlFor={`${prefix}-name`}>Group name</label>
      <input id={`${prefix}-name`} required maxLength={100} value={name} autoFocus={autoFocus}
        onChange={event => onName(event.target.value)} />
      <label htmlFor={`${prefix}-description`}>Description (optional)</label>
      <textarea id={`${prefix}-description`} rows={3} maxLength={500} value={description}
        onChange={event => onDescription(event.target.value)} />
    </>
  )
}
