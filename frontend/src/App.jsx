import { useCallback, useEffect, useState } from 'react'
import { demoDashboard, formatMoney } from './dashboard.js'
import { listGroups } from './groups.js'
import { GroupLink } from './group-ui.jsx'
import { useGroupResource } from './useGroupResource.js'
import { GroupCards, GroupInvitationPage, GroupsPage } from './GroupsPage.jsx'
import GroupPage from './GroupPage.jsx'
import './App.css'
import './Groups.css'

const iconPaths = {
  home: 'm3 10 9-7 9 7M5 9v12h5v-7h4v7h5V9',
  groups: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M16 3a4 4 0 0 1 0 8M22 21v-2a4 4 0 0 0-3-3.87',
  receipt: 'M6 3h12v18l-3-2-3 2-3-2-3 2V3ZM9 7h6M9 11h6',
}

function Icon({ name }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d={iconPaths[name]} />
      {name === 'groups' && <circle cx="9" cy="7" r="4" />}
    </svg>
  )
}

function Dashboard({ dashboard, groups, navigate }) {
  const [showAllExpenses, setShowAllExpenses] = useState(false)
  const { user, summary, expenses } = dashboard
  const visibleExpenses = showAllExpenses ? expenses : expenses.slice(0, 2)
  const currentGroups = groups.data?.filter(group => group.status === 'ACTIVE') ?? []
  const money = (cents) => formatMoney(cents, dashboard.currency)

  return (
    <main id="main-content" className="home" tabIndex={-1}>
        <section className="balance-panel" aria-labelledby="balance-heading">
          <header className="greeting">
            <div className="avatar" aria-hidden="true">{user.name.slice(0, 1)}</div>
            <div>
              <p>Hello,</p>
              <h1>{user.name}</h1>
            </div>
            <span className="brand">Expense<br />Splitter</span>
          </header>
          <div className="balance">
            <h2 id="balance-heading">Your balance</h2>
            <strong className="balance-value">
              {money(summary.receivableCents - summary.payableCents)}
            </strong>
            <p className="balance-caption">Across your shared expenses</p>
          </div>
          <div className="balance-summary">
            <div>
              <p>You are owed</p>
              <strong>{money(summary.receivableCents)}</strong>
            </div>
            <div>
              <p>You owe</p>
              <strong>{money(summary.payableCents)}</strong>
            </div>
          </div>
        </section>

        <p className="demo-note">Demo preview · sample data</p>

        <section id="statistici" className="stats" aria-label="Your overview">
          <article className="stat-card stat-orange">
            <Icon name="groups" />
            <strong>{groups.data ? groups.data.length : '—'}</strong>
            <h2>Groups</h2>
          </article>
          <article className="stat-card stat-purple">
            <Icon name="receipt" />
            <strong>{expenses.length}</strong>
            <h2>Expenses</h2>
          </article>
          <article className="stat-card stat-green">
            <Icon name="receipt" />
            <strong className="stat-total">{money(summary.totalExpensesCents)}</strong>
            <h2>Group spending</h2>
          </article>
        </section>

        <section id="cheltuieli" className="transactions" aria-labelledby="expenses-heading" tabIndex={-1}>
          <div className="section-heading">
            <h2 id="expenses-heading">Recent expenses</h2>
            {expenses.length > 2 && (
              <button type="button" className="text-button" aria-expanded={showAllExpenses}
                aria-controls="expense-list" onClick={() => setShowAllExpenses(!showAllExpenses)}>
                {showAllExpenses ? 'Show less' : 'See all'}
              </button>
            )}
          </div>
          {expenses.length === 0 ? (
            <p className="empty-state">No shared expenses yet.</p>
          ) : (
            <ul id="expense-list" className="transaction-list">
              {visibleExpenses.map((expense) => (
                <li key={expense.id} className="transaction">
                  <span className="transaction-icon" aria-hidden="true"><Icon name="receipt" /></span>
                  <div className="transaction-details">
                    <h3>{expense.description}</h3>
                    <p>{expense.groupName} · Paid by {expense.paidBy}</p>
                    <time dateTime={expense.date}>{expense.date}</time>
                  </div>
                  <strong>{money(expense.amountCents)}</strong>
                </li>
              ))}
            </ul>
          )}
        </section>

      <section className="groups-section" aria-labelledby="groups-heading">
        <div className="section-heading">
          <h2 id="groups-heading">Your groups</h2>
          <GroupLink href="/groups" className="text-button" navigate={navigate}>View groups</GroupLink>
        </div>
        {groups.error && <p className="group-form-error" role="alert">{groups.error.message}</p>}
        {!groups.data && !groups.error && <p className="empty-state" role="status">Loading groups…</p>}
        {groups.data && (currentGroups.length ? <GroupCards groups={currentGroups.slice(0, 3)} navigate={navigate} />
          : <p className="empty-state">You have no active groups yet.</p>)}
      </section>
    </main>
  )
}

function currentLocation() {
  return window.location.pathname + window.location.hash
}

export default function App({ dashboard = demoDashboard }) {
  const [location, setLocation] = useState(currentLocation)
  const [notice, setNotice] = useState('')
  const groups = useGroupResource(listGroups)
  const { refresh, replace: replaceGroups } = groups

  const navigate = useCallback((href, { replace = false, message = '' } = {}) => {
    setNotice(message)
    if (href === currentLocation()) return
    if (replace) window.history.replaceState(null, '', href)
    else window.history.pushState(null, '', href)
    setLocation(currentLocation())
  }, [])

  useEffect(() => {
    function syncLocation() {
      setNotice('')
      setLocation(currentLocation())
    }
    window.addEventListener('popstate', syncLocation)
    window.addEventListener('hashchange', syncLocation)
    return () => {
      window.removeEventListener('popstate', syncLocation)
      window.removeEventListener('hashchange', syncLocation)
    }
  }, [])
  useEffect(() => {
    const target = document.getElementById(location === '/#cheltuieli' ? 'cheltuieli' : 'main-content')
    if (!location.endsWith('/chat')) target?.focus({ preventScroll: true })
    if (location === '/#cheltuieli') target?.scrollIntoView()
    else window.scrollTo({ top: 0, behavior: 'instant' })
  }, [location])

  const exitGroup = useCallback((id, message) => {
    replaceGroups(current => current?.filter(group => String(group.id) !== String(id)) ?? null)
    refresh()
    navigate('/groups', { replace: true, message })
  }, [navigate, refresh, replaceGroups])

  function saveGroup(group) {
    replaceGroups(current => current ? [group, ...current.filter(item => item.id !== group.id)] : null)
    refresh()
  }

  const groupRoute = location.match(/^\/groups\/(\d+)(?:\/(members|invitations|activity|chat))?\/?$/)
  const inviteRoute = location.match(/^\/group-invitations\/([A-Za-z0-9_-]+)\/?$/)
  const groupsSelected = location === '/groups' || location === '/groups/' || !!groupRoute || !!inviteRoute
  const expensesSelected = location === '/#cheltuieli'

  useEffect(() => {
    if (location === '/groups' || location === '/groups/') refresh()
  }, [location, refresh])

  return (
    <div className="app">
      <a className="skip-link" href="#main-content" onClick={event => {
        event.preventDefault()
        document.getElementById('main-content')?.focus()
      }}>Skip to content</a>
      {groupRoute ? <GroupPage key={groupRoute[1]} id={groupRoute[1]} tab={groupRoute[2] ?? 'overview'}
        user={dashboard.user} navigate={navigate} onGroupChanged={saveGroup} onGroupLeft={exitGroup} />
        : inviteRoute ? <GroupInvitationPage key={inviteRoute[1]} token={inviteRoute[1]} navigate={navigate} onGroupAdded={saveGroup} />
          : groupsSelected ? <GroupsPage groups={groups} navigate={navigate} onGroupAdded={saveGroup} notice={notice} />
            : <Dashboard dashboard={dashboard} groups={groups} navigate={navigate} />}
      <nav className="bottom-nav" aria-label="Main navigation">
        <GroupLink href="/" navigate={navigate} aria-current={!expensesSelected && !groupsSelected ? 'page' : undefined}>
          <Icon name="home" /><span>Home</span>
        </GroupLink>
        <GroupLink href="/groups" navigate={navigate} aria-current={groupsSelected ? 'page' : undefined}>
          <Icon name="groups" /><span>Groups</span>
        </GroupLink>
        <GroupLink href="/#cheltuieli" navigate={navigate} aria-current={expensesSelected ? 'location' : undefined}>
          <Icon name="receipt" /><span>Expenses</span>
        </GroupLink>
      </nav>
    </div>
  )
}
