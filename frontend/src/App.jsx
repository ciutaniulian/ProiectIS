import { useState } from 'react'
import { demoDashboard, formatMoney } from './dashboard.js'
import './App.css'

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

// shortcut: demo data until an authenticated dashboard endpoint is available.
function App({ dashboard = demoDashboard }) {
  const [showAllExpenses, setShowAllExpenses] = useState(false)
  const [activeSection, setActiveSection] = useState('acasa')
  const { user, summary, groups, expenses } = dashboard
  const visibleExpenses = showAllExpenses ? expenses : expenses.slice(0, 2)
  const money = (cents) => formatMoney(cents, dashboard.currency)

  return (
    <div className="app">
      <a className="skip-link" href="#acasa">Skip to content</a>
      <main id="acasa" className="home">
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
            <strong>{groups.length}</strong>
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

        <section id="cheltuieli" className="transactions" aria-labelledby="expenses-heading">
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

        <section id="grupuri" className="groups-section" aria-labelledby="groups-heading">
          <div className="section-heading">
            <h2 id="groups-heading">Your groups</h2>
            <span>{groups.length} total</span>
          </div>
          {groups.length === 0 ? (
            <p className="empty-state">You haven't joined a group yet.</p>
          ) : (
            <ul className="group-list">
              {groups.map((group) => (
                <li key={group.id} className="group">
                  <span className="group-avatar" aria-hidden="true">{group.name.slice(0, 1)}</span>
                  <div>
                    <h3>{group.name}</h3>
                    <p>{group.memberCount} members</p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
      <nav className="bottom-nav" aria-label="Main navigation">
        <a href="#acasa" aria-current={activeSection === 'acasa' ? 'location' : undefined} onClick={() => setActiveSection('acasa')}><Icon name="home" /><span>Home</span></a>
        <a href="#grupuri" aria-current={activeSection === 'grupuri' ? 'location' : undefined} onClick={() => setActiveSection('grupuri')}><Icon name="groups" /><span>Groups</span></a>
        <a href="#cheltuieli" aria-current={activeSection === 'cheltuieli' ? 'location' : undefined} onClick={() => setActiveSection('cheltuieli')}><Icon name="receipt" /><span>Expenses</span></a>
      </nav>
    </div>
  )
}

export default App
