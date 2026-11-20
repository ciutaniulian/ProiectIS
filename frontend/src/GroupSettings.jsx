import { useRef, useState } from 'react'
import { GroupFields } from './group-ui.jsx'
import { formatGroupDate } from './groups.js'

function EditGroupForm({ group, busy, onSave, onCancel }) {
  const [name, setName] = useState(group.name)
  const [description, setDescription] = useState(group.description ?? '')
  const changed = name.trim() !== group.name || description.trim() !== (group.description ?? '')

  return (
    <form className="group-form" onSubmit={event => {
      event.preventDefault()
      if (changed && name.trim()) onSave({ name: name.trim(), description: description.trim() })
    }}>
      <fieldset disabled={busy}>
        <GroupFields name={name} description={description} onName={setName} onDescription={setDescription}
          prefix="edit-group" autoFocus />
      </fieldset>
      <div className="group-form-actions">
        <button type="button" className="text-button" disabled={busy} onClick={onCancel}>Cancel</button>
        <button type="submit" className="group-create-submit" disabled={busy || !changed || !name.trim()}>Save changes</button>
      </div>
    </form>
  )
}

export function GroupOverview({ group, busy, save, restore, confirmArchive }) {
  const [editing, setEditing] = useState(false)
  const archived = group.status === 'ARCHIVED'
  return (
    <section className="group-panel" aria-labelledby="group-details-heading">
      <div className="group-section-heading">
        <h2 id="group-details-heading">Group details</h2>
        {!archived && !editing && <button type="button" className="text-button" disabled={busy}
          onClick={() => setEditing(true)}>Edit details</button>}
      </div>
      {editing && !archived ? <EditGroupForm group={group} busy={busy} onCancel={() => setEditing(false)}
        onSave={async data => { if (await save(data)) setEditing(false) }} /> : (
        <p className="group-description">{group.description || 'No group description yet.'}</p>
      )}
      <div className="group-management">
        <h3>{archived ? 'Restore this group' : 'Archive this group'}</h3>
        <p>{archived ? 'Members will be able to make changes and send chat messages again.'
          : 'Members will still be able to read the group and its chat history.'}</p>
        <button type="button" className="group-outline-button" disabled={busy}
          onClick={archived ? restore : confirmArchive}>{archived ? 'Restore group' : 'Archive group'}</button>
      </div>
    </section>
  )
}

export function GroupMembers({ members, user, archived, busy, confirmRemove, confirmLeave }) {
  return (
    <section className="group-panel" aria-labelledby="group-members-heading">
      <div className="group-section-heading">
        <h2 id="group-members-heading">Members</h2>
        <button type="button" className="text-button" disabled={busy} onClick={confirmLeave}>Leave group</button>
      </div>
      <ul className="group-member-list">
        {members.map(member => {
          const ownMembership = String(member.userId) === String(user.id)
          return (
            <li key={member.userId}>
              <div className="group-member-name">
                <span className="group-avatar" aria-hidden="true">{member.name[0]}</span>
                <strong>{member.name}{ownMembership && ' (you)'}</strong>
              </div>
              {!archived && !ownMembership && <button type="button" className="text-button group-danger" disabled={busy}
                aria-label={`Remove ${member.name}`} onClick={() => confirmRemove(member)}>Remove</button>}
            </li>
          )
        })}
      </ul>
    </section>
  )
}

export function GroupInvitations({ invites, archived, busy, create, confirmRevoke }) {
  const [email, setEmail] = useState('')
  const [created, setCreated] = useState(null)
  const [copyStatus, setCopyStatus] = useState('')
  const linkInput = useRef(null)
  const createdInvite = created && invites.some(invitation => invitation.id === created.id) ? created : null
  const inviteLink = createdInvite?.token ? `${window.location.origin}/group-invitations/${encodeURIComponent(createdInvite.token)}` : ''

  async function submit(event) {
    event.preventDefault()
    if (!email.trim() || busy || archived) return
    const invitation = await create(email.trim())
    if (invitation) {
      setEmail('')
      setCreated(invitation)
      setCopyStatus('')
    }
  }

  async function copyLink() {
    try {
      await navigator.clipboard.writeText(inviteLink)
      setCopyStatus('Invitation link copied.')
    } catch {
      linkInput.current?.focus()
      linkInput.current?.select()
      setCopyStatus('The link is selected. Copy it from the field.')
    }
  }

  return (
    <section className="group-panel" aria-labelledby="group-invitations-heading">
      <h2 id="group-invitations-heading">Invitations</h2>
      {!archived ? (
        <>
          <form className="group-form group-invite-form" onSubmit={submit}>
            <label htmlFor="invite-email">Invite by email</label>
            <div className="group-inline-fields">
              <input id="invite-email" required type="email" value={email} maxLength={100} disabled={busy}
                onChange={event => setEmail(event.target.value)} />
              <button type="submit" className="group-create-submit" disabled={busy || !email.trim()}>Create invitation</button>
            </div>
          </form>
          {inviteLink && (
            <div className="group-share-link">
              <p role="status">Invitation created for {createdInvite.email}.</p>
              <label htmlFor="invite-link">Invitation link</label>
              <div className="group-inline-fields">
                <input ref={linkInput} id="invite-link" readOnly value={inviteLink} />
                <button type="button" className="group-outline-button" onClick={copyLink}>Copy invitation link</button>
              </div>
              {copyStatus && <p role="status">{copyStatus}</p>}
            </div>
          )}
        </>
      ) : <p className="group-note">Restore the group to manage invitations.</p>}
      <h3 className="group-pending-heading">Pending invitations</h3>
      {invites.length === 0 ? <p className="empty-state">No pending invitations.</p> : (
        <ul className="group-member-list inbox-list">
          {invites.map(invitation => (
            <li key={invitation.id}>
              <div><strong>{invitation.email}</strong>
                <p>Expires <time dateTime={invitation.expiresAt}>{formatGroupDate(invitation.expiresAt)}</time></p>
              </div>
              {!archived && <button type="button" className="text-button group-danger" disabled={busy}
                aria-label={`Revoke invitation for ${invitation.email}`} onClick={() => confirmRevoke(invitation)}>Revoke</button>}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
