import { useState } from 'react'
import { updateProfile } from './auth.js'
import './ProfilePage.css'

export default function ProfilePage({ user, onSaved, onBack }) {
  const [name, setName] = useState(user.name)
  const [saving, setSaving] = useState(false)
  const [feedback, setFeedback] = useState(null)
  const canSave = name.trim() && name.trim() !== user.name

  async function handleSubmit(event) {
    event.preventDefault()
    if (saving || !canSave) return

    setSaving(true)
    setFeedback(null)
    try {
      const profile = await updateProfile({ name })
      setName(profile.name)
      onSaved(profile)
      setFeedback({ message: 'Profile saved.' })
    } catch (error) {
      setFeedback({ message: error.message, isError: true })
    } finally {
      setSaving(false)
    }
  }

  return (
    <main className="profile-page auth-content">
      <h1>My account</h1>
      <p className="auth-description">Email: {user.email}</p>
      <form onSubmit={handleSubmit}>
        <fieldset disabled={saving}>
          <label htmlFor="profile-name">
            Display name
            <input
              id="profile-name"
              autoComplete="name"
              required
              maxLength={100}
              value={name}
              onChange={(event) => {
                setName(event.target.value)
                setFeedback(null)
              }}
            />
          </label>
          {feedback && (
            <p className={feedback.isError ? 'auth-error' : 'auth-notice'}
              role={feedback.isError ? 'alert' : 'status'}>
              {feedback.message}
            </p>
          )}
          <button className="auth-primary" type="submit" disabled={!canSave}>
            {saving ? 'Saving…' : 'Save changes'}
          </button>
          <button className="text-button" type="button" onClick={onBack}>Back to dashboard</button>
        </fieldset>
      </form>
    </main>
  )
}
