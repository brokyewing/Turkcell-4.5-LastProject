import { useEffect, useState } from 'react'
import { api, setToken, getToken, decodeToken } from './api'
import AdminView from './admin.jsx'
import UserView from './user.jsx'

export default function App() {
  const [authed, setAuthed] = useState(!!getToken())
  // null → role göre başlangıç yüzeyi (kütüphaneci admin, öğrenci user).
  const [surface, setSurface] = useState(null)

  // 401 alındığında api katmanı bu olayı yayınlar; oturumu burada kapatıyoruz.
  useEffect(() => {
    function onLogout() { setAuthed(false); setSurface(null) }
    window.addEventListener('auth:logout', onLogout)
    return () => window.removeEventListener('auth:logout', onLogout)
  }, [])

  if (!authed) return <Login onSuccess={() => setAuthed(true)} />

  const me = decodeToken() || {}
  const isLibrarian = me.role === 'LIBRARIAN'
  const active = surface || (isLibrarian ? 'admin' : 'user')
  function logout() { setToken(null); setAuthed(false); setSurface(null) }

  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand"><span className="dot">K</span> Kitaplık</div>
        <div className="switch">
          <button className={active === 'user' ? 'on' : ''} onClick={() => setSurface('user')}>Üye</button>
          {isLibrarian && (
            <button className={active === 'admin' ? 'on' : ''} onClick={() => setSurface('admin')}>Kütüphaneci</button>
          )}
        </div>
        <div className="who">
          <span className="av">{(me.name || 'K')[0]}</span>
          <span>{me.name || me.sub}</span>
          <button className="btn line sm" onClick={logout}>Çıkış</button>
        </div>
      </header>
      {active === 'admin' && isLibrarian ? <AdminView /> : <UserView me={me} />}
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
