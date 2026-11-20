import { useCallback, useState } from 'react'
import { acceptGroupInvitation, createGroup, declineGroupInvitation, formatGroupDate, getGroupInvitation,
  listGroupInvitations, memberCountText } from './groups.js'
import { GroupFields, GroupLink } from './group-ui.jsx'
import { useGroupResource } from './useGroupResource.js'

export function GroupCards({ groups, navigate }) {
  return (
    <ul className="group-list">
      {groups.map(group => (
        <li key={group.id}>
          <GroupLink className="group group-row-link" href={`/groups/${group.id}`} navigate={navigate}>
            <span className="group-avatar" aria-hidden="true">{group.name[0]}</span>
            <div className="group-details">
              <h3>{group.name}</h3>
              <p>{memberCountText(group.memberCount)}{group.status === 'ARCHIVED' && ' · Archived'}</p>
              {group.description && <p className="group-row-description">{group.description}</p>}
            </div>
            <span className="group-chevron" aria-hidden="true">›</span>
          </GroupLink>
        </li>
      ))}
    </ul>
  )
}

function GroupCreateForm({ onCreated, onCancel }) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event) {
    event.preventDefault()
    if (busy || !name.trim()) return
    setBusy(true)
    setError('')
    try {
      onCreated(await createGroup({ name: name.trim(), description: description.trim() }))
    } catch (failure) {
      setError(failure.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="group-form" onSubmit={submit} aria-label="Create group">
      <fieldset disabled={busy}>
        <GroupFields name={name} description={description} onName={setName} onDescription={setDescription}
          prefix="create-group" autoFocus />
      </fieldset>
      {error && <p className="group-form-error" role="alert">{error}</p>}
      <div className="group-form-actions">
        <button type="button" className="text-button" disabled={busy} onClick={onCancel}>Cancel</button>
        <button type="submit" className="group-create-submit" disabled={busy || !name.trim()}>
          {busy ? 'Creating…' : 'Create group'}
        </button>
      </div>
    </form>
  )
}

function ReceivedInvitations({ invitations, onAccept, onDecline }) {
  const [busyId, setBusyId] = useState(null)
  const [feedback, setFeedback] = useState(null)

  async function decide(invitation, accept) {
    if (busyId !== null) return
    setBusyId(invitation.id)
    setFeedback(null)
    try {
      if (accept) {
        onAccept(await acceptGroupInvitation(invitation.id))
      } else {
        await declineGroupInvitation(invitation.id)
        onDecline(invitation.id)
        setFeedback({ message: 'Invitation declined.' })
      }
    } catch (error) {
      setFeedback({ error: true, message: error.message })
    } finally {
      setBusyId(null)
    }
  }

  return (
    <>
      {feedback && <p className={feedback.error ? 'group-form-error' : 'group-note'}
        role={feedback.error ? 'alert' : 'status'}>{feedback.message}</p>}
      {invitations.length === 0 ? <p className="empty-state">No pending invitations.</p> : (
        <ul className="group-member-list inbox-list">
          {invitations.map(invitation => (
            <li key={invitation.id}>
              <div>
                <h3>{invitation.groupName}</h3>
                <p>Expires <time dateTime={invitation.expiresAt}>{formatGroupDate(invitation.expiresAt)}</time></p>
              </div>
              <div className="group-actions">
                <button type="button" className="text-button" disabled={busyId !== null}
                  onClick={() => decide(invitation, false)}>Decline invitation</button>
                <button type="button" className="group-create-submit" disabled={busyId !== null}
                  onClick={() => decide(invitation, true)}>Accept invitation</button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}

export function GroupsPage({ groups, navigate, onGroupAdded, notice }) {
  const inbox = useGroupResource(listGroupInvitations)
  const [showCreate, setShowCreate] = useState(false)
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('ACTIVE')
  const shown = groups.data?.filter(group => group.status === status
    && group.name.toLocaleLowerCase().includes(query.trim().toLocaleLowerCase())) ?? []

  function selectGroup(group) {
    onGroupAdded(group)
    navigate(`/groups/${group.id}`)
  }

  return (
    <main id="main-content" className="group-page" tabIndex={-1}>
      <header className="group-page-header">
        <h1>Your groups</h1>
        {!showCreate && <button type="button" className="group-create-submit" onClick={() => setShowCreate(true)}>Create group</button>}
      </header>
      {notice && <p className="group-note" role="status">{notice}</p>}
      {showCreate && <GroupCreateForm onCreated={selectGroup} onCancel={() => setShowCreate(false)} />}
      <div className="group-list-controls">
        <label className="group-search" htmlFor="group-search">Search groups
          <input id="group-search" type="search" value={query} placeholder="Search by name"
            onChange={event => setQuery(event.target.value)} />
        </label>
        <div className="group-filter" aria-label="Group status" role="group">
          <button type="button" aria-pressed={status === 'ACTIVE'} onClick={() => setStatus('ACTIVE')}>Active</button>
          <button type="button" aria-pressed={status === 'ARCHIVED'} onClick={() => setStatus('ARCHIVED')}>Archived</button>
        </div>
      </div>
      {groups.error && <p className="group-form-error" role="alert">{groups.error.message}
        <button type="button" className="text-button" onClick={groups.refresh}>Try again</button></p>}
      {!groups.data && !groups.error && <p role="status" className="empty-state">Loading groups…</p>}
      {groups.data && (shown.length > 0 ? <GroupCards groups={shown} navigate={navigate} />
        : <p className="empty-state">{query.trim() ? 'No groups match your search.'
          : status === 'ARCHIVED' ? 'You have no archived groups.' : 'You have no active groups yet.'}</p>)}
      <section className="group-section" aria-labelledby="group-inbox-heading">
        <h2 id="group-inbox-heading">Invitations for you</h2>
        {inbox.error && <p className="group-form-error" role="alert">{inbox.error.message}
          <button type="button" className="text-button" onClick={inbox.refresh}>Try again</button></p>}
        {!inbox.data && !inbox.error && <p className="empty-state" role="status">Loading invitations…</p>}
        {inbox.data && <ReceivedInvitations invitations={inbox.data.filter(invitation => invitation.status === 'PENDING')}
          onAccept={selectGroup} onDecline={id => inbox.replace(inbox.data.filter(invitation => invitation.id !== id))} />}
      </section>
    </main>
  )
}

export function GroupInvitationPage({ token, navigate, onGroupAdded }) {
  const load = useCallback(signal => getGroupInvitation(token, signal), [token])
  const { data: invitation, error } = useGroupResource(load)
  const [declined, setDeclined] = useState(false)

  return (
    <main id="main-content" className="group-page" tabIndex={-1}>
      <GroupLink href="/groups" navigate={navigate} className="group-back-link">‹ Your groups</GroupLink>
      <header className="group-page-header"><h1>Group invitation</h1></header>
      {error && !declined && <p className="group-form-error" role="alert">{error.message}</p>}
      {!invitation && !error && !declined && <p className="empty-state" role="status">Checking invitation…</p>}
      {declined && <p className="group-note" role="status">Invitation declined.</p>}
      {invitation?.status === 'PENDING' && !error && !declined && (
        <ReceivedInvitations invitations={[invitation]} onAccept={group => {
          onGroupAdded(group)
          navigate(`/groups/${group.id}`, { replace: true })
        }} onDecline={() => setDeclined(true)} />
      )}
      {invitation && invitation.status !== 'PENDING' && !declined && !error && (
        <p className="group-note" role="status">This invitation is no longer pending.</p>
      )}
    </main>
  )
}
