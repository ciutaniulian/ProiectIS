import { useCallback, useEffect, useState } from 'react'
import GroupChat from './GroupChat.jsx'
import { GroupInvitations, GroupMembers, GroupOverview } from './GroupSettings.jsx'
import { ConfirmGroupAction, GroupLink } from './group-ui.jsx'
import { useGroupResource } from './useGroupResource.js'
import { archiveGroup, formatGroupDate, getGroup, getGroupActivity, inviteGroupMember, leaveGroup,
  memberCountText, removeGroupMember, restoreGroup, revokeGroupInvitation, updateGroup } from './groups.js'

const tabs = [
  ['overview', 'Overview'], ['members', 'Members'], ['invitations', 'Invitations'], ['activity', 'Activity'], ['chat', 'Chat'],
]

function activityDescription(activity) {
  const action = {
    CREATED: 'created the group', UPDATED: 'edited the group details', JOINED: 'joined the group',
    LEFT: 'left the group', REMOVED: `removed ${activity.subjectName ?? 'a member'}`,
    ARCHIVED: 'archived the group', RESTORED: 'restored the group', CLOSED: 'closed the group',
  }[activity.type]
  return action ? `${activity.actorName ?? 'A member'} ${action}.` : null
}

function GroupActivity({ id, onAccessLost }) {
  const [items, setItems] = useState([])
  const [hasNext, setHasNext] = useState(false)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    getGroupActivity(id, page, controller.signal).then(result => {
      if (controller.signal.aborted) return
      setItems(previous => page === 0 ? result.items : [...previous, ...result.items.filter(item =>
        !previous.some(existing => existing.id === item.id))])
      setHasNext(result.hasNext)
    }).catch(failure => {
      if (controller.signal.aborted) return
      if (failure.code === 'GROUP_FORBIDDEN') onAccessLost()
      else setError(failure.message)
    }).finally(() => {
      if (!controller.signal.aborted) setLoading(false)
    })
    return () => controller.abort()
  }, [id, page, attempt, onAccessLost])

  return (
    <section className="group-panel" aria-labelledby="group-activity-heading">
      <h2 id="group-activity-heading">Group activity</h2>
      {loading && <p className="empty-state" role="status">Loading activity…</p>}
      {error && <p className="group-form-error" role="alert">{error} <button type="button" className="text-button"
        onClick={() => { setError(''); setLoading(true); setAttempt(value => value + 1) }}>Try again</button></p>}
      {items.length === 0 && !loading && !error && <p className="empty-state">No activity yet.</p>}
      <ol className="group-activity-list">
        {items.map(item => (
          <li key={item.id}>
            <p>{activityDescription(item)}</p>
            <time dateTime={item.createdAt}>{formatGroupDate(item.createdAt)}</time>
          </li>
        ))}
      </ol>
      {hasNext && !error && <button type="button" className="group-outline-button" disabled={loading}
        onClick={() => { setLoading(true); setPage(value => value + 1) }}>Load older activity</button>}
    </section>
  )
}

export default function GroupPage({ id, tab, user, navigate, onGroupChanged, onGroupLeft }) {
  const load = useCallback(signal => getGroup(id, signal), [id])
  const resource = useGroupResource(load, 4000)
  const details = resource.data
  const refresh = resource.refresh
  const [action, setAction] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const accessLost = useCallback(() => onGroupLeft(id, 'You no longer have access to this group.'), [id, onGroupLeft])
  const group = details?.group
  const url = `/groups/${id}`
  const archive = group?.status === 'ARCHIVED'
  const canOpen = resource.error?.status !== 403 && resource.error?.status !== 401

  useEffect(() => {
    if (details && resource.error?.status === 403) accessLost()
  }, [details, resource.error, accessLost])

  async function update(operation, { after, onConflict, returnsDetails = true } = {}) {
    if (busy) return null
    setBusy(true)
    setError('')
    try {
      const result = await operation()
      if (returnsDetails) {
        resource.replace(result)
        onGroupChanged(result.group)
      }
      after?.(result)
      return result ?? true
    } catch (failure) {
      if (failure.code === 'GROUP_FORBIDDEN') accessLost()
      else if (!onConflict?.(failure)) {
        setError(failure.message)
        if (failure.status === 409) refresh()
      }
      return null
    } finally {
      setBusy(false)
    }
  }

  function confirm(label, description, operation, options) {
    setError('')
    setAction({ label, description, operation, options })
  }

  async function finishConfirm() {
    if (await update(action.operation, action.options)) setAction(null)
  }

  function prepareLeave(last) {
    confirm(last ? 'Close group' : 'Leave group', last
      ? `You are the last member of ${group.name}. Leaving will close this group permanently.`
      : `Leave ${group.name}? You will lose access to the group and its chat.`,
    () => leaveGroup(id, last), {
      returnsDetails: false,
      after: () => onGroupLeft(id, last ? 'Group closed.' : 'You left the group.'),
      onConflict: failure => {
        if (failure.code !== 'LAST_MEMBER_CONFIRMATION_REQUIRED') return false
        prepareLeave(true)
        return true
      },
    })
  }

  function changeChatStatus(status) {
    if (group.status === status) return
    resource.replace({ ...details, group: { ...group, status } })
    refresh()
  }

  return (
    <main id="main-content" className="group-page" tabIndex={-1}>
      <GroupLink href="/groups" navigate={navigate} className="group-back-link">‹ Your groups</GroupLink>
      {resource.error && <p className="group-form-error" role="alert">{resource.error.message}
        {canOpen && <button type="button" className="text-button" onClick={refresh}>Try again</button>}</p>}
      {!details && !resource.error && <p className="empty-state" role="status">Loading group…</p>}
      {details && canOpen && (
        <>
          <header className="group-page-identity">
            <span className="group-avatar" aria-hidden="true">{group.name[0]}</span>
            <div>
              <h1>{group.name}</h1>
              <p>{memberCountText(group.memberCount)}{archive && ' · Archived'}</p>
            </div>
          </header>
          <nav className="group-tabs" aria-label="Group sections">
            {tabs.map(([key, label]) => (
              <GroupLink key={key} href={key === 'overview' ? url : `${url}/${key}`} navigate={navigate}
                aria-current={key === tab ? 'page' : undefined}>{label}</GroupLink>
            ))}
          </nav>
          {error && !action && <p className="group-form-error" role="alert">{error}</p>}
          {(tab === 'overview' || tab === 'chat') && <GroupOverview group={group} busy={busy}
            save={data => update(() => updateGroup(id, data))} restore={() => update(() => restoreGroup(id))}
            confirmArchive={() => confirm('Archive group', `Archive ${group.name} for all members? They can still view its history.`,
              () => archiveGroup(id))} />}
          {tab === 'members' && <GroupMembers members={details.members} user={user} archived={archive} busy={busy}
            confirmLeave={() => prepareLeave(group.memberCount === 1)} confirmRemove={member => confirm('Remove member',
              `Remove ${member.name} from ${group.name}? They will lose access to the group and its chat.`,
              () => removeGroupMember(id, member.userId))} />}
          {tab === 'invitations' && <GroupInvitations invites={details.invitations} archived={archive} busy={busy}
            create={email => update(() => inviteGroupMember(id, email), {
              returnsDetails: false, after: invitation => resource.replace(current => ({
                ...current,
                invitations: [...current.invitations.filter(item => item.id !== invitation.id), invitation],
              })),
            })} confirmRevoke={invitation => confirm('Revoke invitation', `Revoke the invitation for ${invitation.email}?`,
              () => revokeGroupInvitation(id, invitation.id))} />}
          {tab === 'activity' && <GroupActivity id={id} onAccessLost={accessLost} />}
          {tab === 'chat' && <GroupChat group={group} currentUser={user} onAccessLost={accessLost}
            onGroupStateChanged={changeChatStatus} onClose={() => navigate(url, { replace: true })} />}
          {action && <ConfirmGroupAction action={action} busy={busy} error={error} onConfirm={finishConfirm}
            onCancel={() => { setAction(null); setError('') }} />}
        </>
      )}
    </main>
  )
}
