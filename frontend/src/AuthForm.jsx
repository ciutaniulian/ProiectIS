import { useRef, useState } from 'react'
import { login, register } from './auth.js'

export default function AuthForm({ onAuthenticated }) {
  const [mode, setMode] = useState('login')
  const [email, setEmail] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const passwordInput = useRef(null)
  const registering = mode === 'register'

  function changeMode() {
    setShowPassword(false)
    setMode(registering ? 'login' : 'register')
    setError('')
    setNotice('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (busy) return
    const form = event.currentTarget
    const fields = new FormData(form)
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const credentials = { email, password: fields.get('password') }
      if (registering) {
        await register({ ...credentials, name: fields.get('name') })
        form.reset()
        setMode('login')
        setNotice('Account created. Log in with your email and password.')
        passwordInput.current?.focus()
      } else {
        const profile = await login({ ...credentials, rememberMe: fields.get('rememberMe') === 'on' })
        form.reset()
        onAuthenticated(profile)
      }
    } catch (failure) {
      setError(failure.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="auth-page">
      <header className="auth-brand">
        <span className="brand-lockup"><span className="brand-mark" aria-hidden="true"><i /><i /></span>Expense Splitter</span>
        <div className="auth-intro"><p>Shared moments.<br />Shared expenses.</p><span>A little less keeping track. A little more time together.</span></div>
      </header>
      <section className="auth-content" aria-labelledby="auth-heading">
        <h1 id="auth-heading">{registering ? 'Share more. Settle simply.' : 'Welcome back.'}</h1>
        <p className="auth-description">{registering ? 'Create an account to start splitting expenses together.' : 'Log in to your Expense Splitter account.'}</p>
        {notice && <p className="auth-notice" role="status">{notice}</p>}
        {error && <p className="auth-error" role="alert" id="auth-error">{error}</p>}
        <form onSubmit={handleSubmit} aria-describedby={error ? 'auth-error' : undefined} aria-busy={busy}>
          <fieldset disabled={busy}>
            {registering && <label htmlFor="name">Name
              <input id="name" name="name" autoComplete="name" required maxLength={100} pattern=".*\S.*" />
            </label>}
            <label htmlFor="email">Email
              <input id="email" name="email" type="email" autoComplete="email" required maxLength={100}
                value={email} onChange={(event) => setEmail(event.target.value)} />
            </label>
            <div className="password-field"><label htmlFor="password">Password</label>
              <div className="password-control"><input key={mode} id="password" name="password" ref={passwordInput} type={showPassword ? 'text' : 'password'} required
                autoComplete={registering ? 'new-password' : 'current-password'}
                minLength={registering ? 6 : undefined} maxLength={registering ? 64 : undefined}
                aria-describedby={registering ? 'password-hint' : undefined} />
                <button type="button" className="password-toggle" aria-label={showPassword ? 'Hide password' : 'Show password'} aria-pressed={showPassword} onClick={() => setShowPassword(!showPassword)}>{showPassword ? 'Hide' : 'Show'}</button>
              </div>
            </div>
            {registering ? <p id="password-hint" className="auth-hint">Use 6–64 characters.</p> : (
              <label className="auth-checkbox" htmlFor="rememberMe">
                <input id="rememberMe" name="rememberMe" type="checkbox" /> Keep me signed in for 7 days
              </label>
            )}
            <button className="auth-primary" type="submit">{busy ? (registering ? 'Creating account…' : 'Logging in…') : (registering ? 'Create account' : 'Log in')}</button>
          </fieldset>
        </form>
        <div className="auth-switch">
          <span>{registering ? 'Already have an account?' : 'New to Expense Splitter?'}</span>
          <button type="button" className="text-button" onClick={changeMode} disabled={busy}>{registering ? 'Log in' : 'Create an account'}</button>
        </div>
      </section>
    </main>
  )
}
