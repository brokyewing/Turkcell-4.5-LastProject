import { useState } from 'react'
import { api, setToken, getToken, decodeToken } from './api'
import AdminView from './admin.jsx'
import UserView from './user.jsx'

export default function App() {
  const [authed, setAuthed] = useState(!!getToken())
  const [surface, setSurface] = useState('user')

  if (!authed) return <Login onSuccess={() => setAuthed(true)} />

  const me = decodeToken() || {}
  function logout() { setToken(null); setAuthed(false) }

  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand"><span className="dot">K</span> Kitaplık</div>
        <div className="switch">
          <button className={surface === 'user' ? 'on' : ''} onClick={() => setSurface('user')}>Üye</button>
          <button className={surface === 'admin' ? 'on' : ''} onClick={() => setSurface('admin')}>Kütüphaneci</button>
        </div>
        <div className="who">
          <span className="av">{(me.name || 'K')[0]}</span>
          <span>{me.name || me.sub}</span>
          <button className="btn line sm" onClick={logout}>Çıkış</button>
        </div>
      </header>
      {surface === 'user' ? <UserView me={me} /> : <AdminView />}
    </div>
  )
}

function Login({ onSuccess }) {
  const [studentNumber, setStudentNumber] = useState('S1001')
  const [password, setPassword] = useState('pass123')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError(null); setLoading(true)
    try {
      const res = await api.login(studentNumber, password)
      setToken(res.token)
      onSuccess()
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-wrap">
      <div className="login-card rise">
        <div className="brand"><span className="dot">K</span> Kitaplık</div>
        <div className="sub">Kütüphanene giriş yap.</div>
        <form onSubmit={submit}>
          <div>
            <label>Öğrenci numarası</label>
            <input value={studentNumber} onChange={(e) => setStudentNumber(e.target.value)} autoFocus />
          </div>
          <div>
            <label>Parola</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
          {error && <div className="err">{error}</div>}
          <button className="btn primary" type="submit" disabled={loading} style={{ justifyContent: 'center' }}>
            {loading ? <><span className="spinner" /> Giriş yapılıyor…</> : 'Giriş yap'}
          </button>
        </form>
        <div className="hint">Demo: S1001 / pass123 · JWT gateway'de doğrulanır</div>
      </div>
    </div>
  )
}
