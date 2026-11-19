import { useEffect, useState } from 'react'
import App from './App.jsx'
import AuthForm from './AuthForm.jsx'
import ProfilePage from './ProfilePage.jsx'
import { getProfile, logout } from './auth.js'
import { demoDashboard } from './dashboard.js'
import './Auth.css'

export default function AuthGateway() {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const [loggingOut, setLoggingOut] = useState(false)
  const [showProfile, setShowProfile] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    getProfile(controller.signal)
      .then((profile) => { if (!controller.signal.aborted) setUser(profile) })
      .catch((failure) => { if (!controller.signal.aborted) setError(failure.message) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [attempt])

  async function handleLogout() {
    setLoggingOut(true)
    setError('')
    try {
      await logout()
      setUser(null)
      setShowProfile(false)
    } catch (failure) {
      setError(failure.message)
    } finally {
      setLoggingOut(false)
    }
  }

  if (loading) return <main className="auth-page auth-wait" role="status">Opening Expense Splitter…</main>

  if (!user && error) return (
    <main className="auth-page auth-wait">
      <h1>Unable to open your account</h1>
      <p className="auth-error" role="alert">{error}</p>
      <button className="auth-primary" onClick={() => { setLoading(true); setError(''); setAttempt(attempt + 1) }}>Try again</button>
    </main>
  )

  if (!user) return <AuthForm onAuthenticated={setUser} />

  return (
    <>
      <div className="account-bar">
        <span>Signed in as <strong>{user.email}</strong></span>
        {!showProfile && <button type="button" className="text-button" onClick={() => setShowProfile(true)}>My account</button>}
        <button type="button" className="text-button" onClick={handleLogout} disabled={loggingOut}>{loggingOut ? 'Logging out…' : 'Log out'}</button>
        {error && <p className="auth-error" role="alert">{error}</p>}
      </div>
      {showProfile ? (
        <ProfilePage
          user={user}
          onSaved={(profile) => setUser((current) => current?.id === profile.id ? profile : current)}
          onBack={() => setShowProfile(false)}
        />
      ) : (
        <App dashboard={{ ...demoDashboard, user }} />
      )}
    </>
  )
}
