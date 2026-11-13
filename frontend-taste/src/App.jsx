import { useEffect, useState } from 'react'
import { api, setToken, getToken } from './api'
import { decodeToken } from './utils/jwt'
import AdminView from './admin.jsx'
import UserView from './user.jsx'
import { Mark } from './ui.jsx'

export default function App() {
  const [authed, setAuthed] = useState(!!getToken())
  const [surface, setSurface] = useState(null)

  // 401 alındığında api katmanı bu olayı yayınlar; oturumu burada kapatıyoruz.
  useEffect(() => {
    function onLogout() {
      setAuthed(false)
      setSurface(null)
    }
    window.addEventListener('auth:logout', onLogout)
    return () => window.removeEventListener('auth:logout', onLogout)
  }, [])

  if (!authed) return <Login onSuccess={() => setAuthed(true)} />

  const me = decodeToken(getToken()) || {}
  const isLibrarian = me.role === 'LIBRARIAN'
  const active = surface || (isLibrarian ? 'desk' : 'discover')

  function logout() {
    setToken(null)
    setAuthed(false)
    setSurface(null)
  }

  const roleLabel = isLibrarian ? 'Kütüphaneci' : 'Üye'

  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          <Mark />
          <div>
            <div className="name">Kitaplık</div>
            <span className="tag">OKUMA ODASI</span>
          </div>
        </div>

        <nav className="nav" aria-label="Ana gezinme">
          <button className={active === 'discover' ? 'on' : ''} onClick={() => setSurface('discover')}>
            Keşfet
          </button>
          <button className={active === 'loans' ? 'on' : ''} onClick={() => setSurface('loans')}>
            Kitaplarım
          </button>
          {isLibrarian && (
            <button className={active === 'desk' ? 'on' : ''} onClick={() => setSurface('desk')}>
              Kütüphane Masası
            </button>
          )}
        </nav>

        <div className="spacer" />

        <div className="userchip">
          <span className="av">{(me.name || me.sub || 'K')[0]}</span>
          <span className="id">
            <b>{me.name || me.sub}</b>
            <span>{roleLabel}</span>
          </span>
        </div>
        <button className="icon-btn out" onClick={logout} title="Çıkış yap" aria-label="Çıkış yap">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M15 17l5-5-5-5" />
            <path d="M20 12H9" />
            <path d="M9 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h3" />
          </svg>
        </button>
      </header>

      {active === 'desk' && isLibrarian ? (
        <AdminView me={me} />
      ) : (
        <UserView me={me} tab={active === 'loans' ? 'loans' : 'discover'} onNavigate={setSurface} />
      )}
    </div>
  )
}

/* ==========================================================================
   Giriş ekranı
   ========================================================================== */

const SHELF = [
  { title: '1984', h: '78%', bg: '#8f3524' },
  { title: 'Hayvan Çiftliği', h: '90%', bg: '#22473c' },
  { title: 'Kürk Mantolu Madonna', h: '70%', bg: '#8a6a22' },
  { title: 'Tutunamayanlar', h: '95%', bg: '#5d3b52' },
  { title: 'Saatleri Ayarlama Enstitüsü', h: '83%', bg: '#3c4a55' },
  { title: 'Sefiller', h: '75%', bg: '#2b2721' },
  { title: 'Kuyucaklı Yusuf', h: '88%', bg: '#93412c' },
]

function Login({ onSuccess }) {
  const [studentNumber, setStudentNumber] = useState('')
  const [password, setPassword] = useState('')
  const [showPw, setShowPw] = useState(false)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const res = await api.login(studentNumber.trim(), password)
      setToken(res.token)
      onSuccess()
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  function fill(no, pw) {
    setStudentNumber(no)
    setPassword(pw)
    setError(null)
  }

  return (
    <div className="gate">
      <aside className="gate-art">
        <div className="gate-brand">
          <Mark />
          <span>Kitaplık</span>
        </div>

        <div className="gate-copy">
          <h1 className="h1">
            Her kitap
            <br />
            <span className="em">yeni bir</span>
            <br />
            okuma odası.
          </h1>
          <p>
            Katalogdan seç, ödünç al, iadeyi takip et. Kütüphanenin tamamı tek bir sakin
            yüzeyde.
          </p>
        </div>

        <div>
          <div className="shelf" aria-hidden="true">
            {SHELF.map((b) => (
              <div
                key={b.title}
                className="bk"
                style={{
                  height: b.h,
                  background: `linear-gradient(90deg, rgba(0,0,0,.28), rgba(255,255,255,.06) 26%, transparent 60%), ${b.bg}`,
                }}
              >
                <span>{b.title}</span>
              </div>
            ))}
          </div>
          <div className="gate-foot" style={{ marginTop: 14 }}>
            <span>Turkcell Akademi</span>
            <span>Bitirme Projesi</span>
            <span>Mikroservis · 2025</span>
          </div>
        </div>
      </aside>

      <div className="gate-form">
        <div className="form-wrap">
          <div className="form-head">
            <span className="eyebrow">ÜYE GİRİŞİ</span>
            <h2 className="h2">Tekrar hoş geldin.</h2>
            <p>Öğrenci numaranla kütüphanene eriş.</p>
          </div>

          <form className="login-form" onSubmit={submit}>
            <div className="field">
              <label className="field-label" htmlFor="login-no">
                Öğrenci numarası
              </label>
              <input
                id="login-no"
                value={studentNumber}
                onChange={(e) => setStudentNumber(e.target.value)}
                placeholder="S1001"
                autoComplete="username"
                autoFocus
                required
              />
            </div>

            <div className="field">
              <label className="field-label" htmlFor="login-pw">
                Parola
              </label>
              <div className="pw-wrap">
                <input
                  id="login-pw"
                  type={showPw ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  autoComplete="current-password"
                  required
                />
                <button
                  type="button"
                  className="pw-toggle"
                  onClick={() => setShowPw((v) => !v)}
                  aria-label={showPw ? 'Parolayı gizle' : 'Parolayı göster'}
                  title={showPw ? 'Parolayı gizle' : 'Parolayı göster'}
                >
                  {showPw ? (
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round">
                      <path d="M3 3l18 18" />
                      <path d="M10.6 10.6a2 2 0 0 0 2.8 2.8" />
                      <path d="M9.4 5.2A9.9 9.9 0 0 1 12 5c5 0 9 4.5 9 7 0 .9-.6 2-1.6 3.1" />
                      <path d="M6.2 6.6C3.9 8 2.1 10.2 2.1 12c0 2.5 4 7 9.9 7 1.4 0 2.7-.3 3.8-.8" />
                    </svg>
                  ) : (
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round">
                      <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12Z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  )}
                </button>
              </div>
            </div>

            {error && (
              <div className="form-err" role="alert">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" style={{ flex: '0 0 16px', marginTop: 1 }}>
                  <circle cx="12" cy="12" r="9" />
                  <path d="M12 8v5M12 16.5v.01" />
                </svg>
                <span>{error}</span>
              </div>
            )}

            <button className="btn primary block" type="submit" disabled={loading}>
              {loading ? (
                <>
                  <span className="spinner" /> Giriş yapılıyor…
                </>
              ) : (
                <>
                  Giriş yap
                  <svg className="arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M5 12h14M13 6l6 6-6 6" />
                  </svg>
                </>
              )}
            </button>
          </form>

          <div className="demo">
            <div className="demo-h">
              <span className="mono" style={{ letterSpacing: '0.14em' }}>
                DEMO HESAPLAR
              </span>
              <span className="mono" style={{ letterSpacing: '0.06em', textTransform: 'none' }}>
                doldur →
              </span>
            </div>
            <div className="demo-row">
              <span className="who">
                <b>Öğrenci</b>
                <span>S1001 · pass123</span>
              </span>
              <button className="btn line sm" type="button" onClick={() => fill('S1001', 'pass123')}>
                Doldur
              </button>
            </div>
            <div className="demo-row">
              <span className="who">
                <b>Kütüphaneci</b>
                <span>L1001 · librarian123</span>
              </span>
              <button className="btn line sm" type="button" onClick={() => fill('L1001', 'librarian123')}>
                Doldur
              </button>
            </div>
          </div>

          <div className="gate-legal">JWT · Spring Cloud Gateway · Tek origin</div>
        </div>
      </div>
    </div>
  )
}
