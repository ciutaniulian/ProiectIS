import { useEffect, useState } from 'react'
import GroupChat from './GroupChat.jsx'
import {
  addGroupMember, createGroup, getGroupMembers, getGroups,
  leaveGroup, promoteGroupMember, removeGroupMember,
} from './groups.js'
import './GroupsSection.css'

export default function GroupsSection({ user, onCountChange }) {
  const [groups, setGroups] = useState(null)
  const [selected, setSelected] = useState(null)
  const [attempt, setAttempt] = useState(0)
  const [loadError, setLoadError] = useState('')
  const [submitError, setSubmitError] = useState('')
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    getGroups(controller.signal)
      .then((data) => {
        if (controller.signal.aborted) return
        setGroups(data)
        onCountChange(data.length)
      })
      .catch((error) => {
        if (!controller.signal.aborted) setLoadError(error.message)
      })
    return () => controller.abort()
  }, [attempt, onCountChange])

  function refreshGroups() {
    setLoadError('')
    setAttempt((value) => value + 1)
  }

  async function handleCreate(event) {
    event.preventDefault()
    const fields = new FormData(event.currentTarget)
    const form = event.currentTarget
    const name = fields.get('name').trim()
    if (creating || !name) return

    setCreating(true)
    setSubmitError('')
    try {
      const group = await createGroup({ name, description: fields.get('description').trim() })
      form.reset()
      refreshGroups()
      setSelected(group)
    } catch (error) {
      setSubmitError(error.message)
    } finally {
      setCreating(false)
    }
  }

  function closeGroup() {
    setSelected(null)
    refreshGroups()
  }

  return (
    <section id="grupuri" className="groups-section" aria-labelledby="groups-heading">
      {selected ? (
        <GroupDetails key={selected.id} group={selected} user={user} onClose={closeGroup} />
      ) : (
        <>
          <h2 id="groups-heading">Your groups</h2>
          <form className="group-form" onSubmit={handleCreate}>
            <label>Group name<input name="name" maxLength={100} required /></label>
            <label>Description <span>(optional)</span><input name="description" maxLength={100} /></label>
            <button type="submit" className="auth-primary" disabled={creating}>
              {creating ? 'Creating…' : 'Create group'}
            </button>
          </form>
          {submitError && <p className="auth-error" role="alert">{submitError}</p>}
          {loadError && (
            <div role="alert">
              <p className="auth-error">{loadError}</p>
              <button type="button" className="text-button" onClick={refreshGroups}>Try again</button>
            </div>
          )}
          {!groups && !loadError && <p role="status">Loading groups…</p>}
          {groups?.length === 0 && <p className="empty-state">You haven't joined a group yet.</p>}
          {!!groups?.length && (
            <ul className="group-list">
              {groups.map((group) => (
                <li key={group.id} className="group">
                  <span className="group-avatar" aria-hidden="true">{group.name.slice(0, 1)}</span>
                  <div className="group-entry">
                    <h3>{group.name}</h3>
                    <p>{group.role === 'ADMIN' ? 'Administrator' : 'Member'}</p>
                  </div>
                  <button type="button" className="text-button" onClick={() => setSelected(group)}>
                    Open<span className="group-sr-only"> {group.name}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  )
}

function GroupDetails({ group, user, onClose }) {
  const [members, setMembers] = useState(null)
  const [attempt, setAttempt] = useState(0)
  const [loadError, setLoadError] = useState('')
  const [actionError, setActionError] = useState('')
  const [pending, setPending] = useState(false)
  const [chatOpen, setChatOpen] = useState(false)
  const canManage = members?.some((member) => member.userId === user.id && member.role === 'ADMIN')

  useEffect(() => {
    const controller = new AbortController()
    getGroupMembers(group.id, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setMembers(data)
      })
      .catch((error) => {
        if (controller.signal.aborted) return
        setMembers(null)
        setLoadError(error.message)
      })
    return () => controller.abort()
  }, [group.id, attempt])

  function refreshMembers() {
    setLoadError('')
    setAttempt((value) => value + 1)
  }

  async function perform(action, onSuccess = refreshMembers) {
    if (pending) return
    setPending(true)
    setActionError('')
    try {
      await action()
      onSuccess()
    } catch (error) {
      setActionError(error.message)
      refreshMembers()
    } finally {
      setPending(false)
    }
  }

  function handleAdd(event) {
    event.preventDefault()
    const form = event.currentTarget
    const email = new FormData(form).get('email').trim()
    perform(() => addGroupMember(group.id, email), () => {
      form.reset()
      refreshMembers()
    })
  }

  return (
    <>
      <button type="button" className="text-button" onClick={onClose}>Back to groups</button>
      <h2 id="groups-heading">{group.name}</h2>
      {group.description && <p>{group.description}</p>}
      {actionError && <p className="auth-error" role="alert">{actionError}</p>}
      {loadError && (
        <div role="alert">
          <p className="auth-error">{loadError}</p>
          <button type="button" className="text-button" onClick={refreshMembers}>Try again</button>
        </div>
      )}
      {!members && !loadError && <p role="status">Loading members…</p>}
      {members && (
        <>
          <div className="group-actions">
            <button type="button" className="text-button" onClick={() => setChatOpen(true)}>Chat</button>
            <button type="button" className="text-button group-danger" disabled={pending} onClick={() => {
              if (window.confirm(`Leave ${group.name}?`)) perform(() => leaveGroup(group.id), onClose)
            }}>Leave group</button>
          </div>
          <form className="group-form" onSubmit={handleAdd}>
            <label>Member email<input type="email" name="email" required /></label>
            <button type="submit" className="auth-primary" disabled={pending}>Add member</button>
          </form>
          <h3>Members</h3>
          <ul className="group-list">
            {members.map((member) => (
              <li className="group group-member" key={member.userId}>
                <div className="group-entry">
                  <h4>{member.name}{member.userId === user.id ? ' (you)' : ''}</h4>
                  <p>{member.role === 'ADMIN' ? 'Administrator' : 'Member'}</p>
                </div>
                {canManage && member.userId !== user.id && (
                  <div className="group-actions">
                    {member.role === 'MEMBER' && (
                      <button type="button" className="text-button" disabled={pending}
                        aria-label={`Make ${member.name} an administrator`}
                        onClick={() => perform(() => promoteGroupMember(group.id, member.userId))}>
                        Make admin
                      </button>
                    )}
                    <button type="button" className="text-button group-danger" disabled={pending}
                      aria-label={`Remove ${member.name} from the group`}
                      onClick={() => {
                        if (window.confirm(`Remove ${member.name} from ${group.name}?`)) {
                          perform(() => removeGroupMember(group.id, member.userId))
                        }
                      }}>Remove</button>
                  </div>
                )}
              </li>
            ))}
          </ul>
          {chatOpen && <GroupChat group={{ ...group, memberCount: members.length }}
            currentUser={user} onClose={() => setChatOpen(false)} />}
        </>
      )}
    </>
  )
}
