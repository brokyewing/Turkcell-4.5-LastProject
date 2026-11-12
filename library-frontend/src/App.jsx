import { useState, useEffect } from 'react'
import { DataProvider, useData } from './data'
import { ToastProvider } from './ui'
import { Icon } from './icons'
import { Dashboard, BooksView, AuthorsView, CategoriesView } from './views'
import { api, setToken, isAuthed, getToken } from './api'
import { decodeToken } from './utils/jwt'

const NAV = [
  { key: 'dashboard', label: 'Genel Bakış', icon: Icon.Dashboard },
  { key: 'books', label: 'Kitaplar', icon: Icon.Book, countKey: 'books' },
  { key: 'authors', label: 'Yazarlar', icon: Icon.Users, countKey: 'authors' },
  { key: 'categories', label: 'Kategoriler', icon: Icon.Tag, countKey: 'categories' },
]

const TITLES = {
  dashboard: ['Genel Bakış', 'Kütüphanenin anlık durumu'],
  books: ['Kitaplar', 'Katalog yönetimi'],
  authors: ['Yazarlar', 'Yazar kayıtları'],
  categories: ['Kategoriler', 'Sınıflandırma'],
}

// Tema tercihini localStorage'da tut, ilk açılışta sistem tercihini kullan.
function useTheme() {
  const [theme, setTheme] = useState(() => {
    const saved = localStorage.getItem('theme')
    if (saved) return saved
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  })
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('theme', theme)
  }, [theme])
  return [theme, () => setTheme((t) => (t === 'dark' ? 'light' : 'dark'))]
}

function Sidebar({ active, onNav }) {
  const { books, authors, categories } = useData()
  const counts = { books: books.length, authors: authors.length, categories: categories.length }
  return (
    <aside className="sidebar">
      <div className="brand">
        <span className="logo"><Icon.Book size={20} /></span>
        <span>LibraryApp<small>Yönetim Paneli</small></span>
      </div>
      <nav className="nav">
        {NAV.map((n) => (
          <button key={n.key} className={`nav-item ${active === n.key ? 'active' : ''}`} onClick={() => onNav(n.key)}>
            <n.icon size={18} />
            <span>{n.label}</span>
            {n.countKey && <span className="count">{counts[n.countKey]}</span>}
          </button>
        ))}
      </nav>
    </aside>
  )
}

function Shell({ onLogout }) {
  const [active, setActive] = useState('dashboard')
  const [theme, toggleTheme] = useTheme()
  const [title, sub] = TITLES[active]

  return (
    <div className="shell">
      <Sidebar active={active} onNav={setActive} />
      <div className="main">
        <header className="topbar">
          <div>
            <h1>{title}</h1>
            <div className="sub">{sub}</div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn ghost" onClick={onLogout} title="Oturumu kapat">Çıkış</button>
            <button className="btn icon" onClick={toggleTheme} aria-label={theme === 'dark' ? 'Açık moda geç' : 'Koyu moda geç'} title="Tema değiştir">
              {theme === 'dark' ? <Icon.Sun size={18} /> : <Icon.Moon size={18} />}
            </button>
          </div>
        </header>
        <div className="content">
          {active === 'dashboard' && <Dashboard />}
          {active === 'books' && <BooksView />}
          {active === 'authors' && <AuthorsView />}
          {active === 'categories' && <CategoriesView />}
        </div>
      </div>
    </div>
  )
}

export default function App() {
  const [authed, setAuthed] = useState(isAuthed())

  // 401 alındığında api katmanı bu olayı yayınlar; oturumu burada kapatıyoruz.
  useEffect(() => {
    function onLogout() { setAuthed(false) }
    window.addEventListener('auth:logout', onLogout)
    return () => window.removeEventListener('auth:logout', onLogout)
  }, [])

  if (!authed) return <Login onSuccess={() => setAuthed(true)} />

  const me = decodeToken(getToken()) || {}
  const logout = () => { setToken(null); setAuthed(false) }

  // Yönetim paneli yalnız kütüphaneci hesaplara açık.
  if (me.role !== 'LIBRARIAN') return <Forbidden me={me} onLogout={logout} />

  return (
    <ToastProvider>
      <DataProvider>
        <Shell onLogout={logout} />
      </DataProvider>
    </ToastProvider>
  )
}

/* Rol kapısı: STUDENT (veya role claim'i olmayan) hesaplara sade yetkisiz ekranı. */
function Forbidden({ me, onLogout }) {
  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <div className="brand">
          <span className="logo"><Icon.Book size={20} /></span>
          <span>LibraryApp<small>Yönetim Paneli</small></span>
        </div>
        <h1 style={{ fontSize: 20, margin: '20px 0 8px' }}>Yönetim paneline erişim yok</h1>
        <p className="sub" style={{ textAlign: 'left', marginTop: 0 }}>
          Bu panel kütüphaneci hesapları içindir.
        </p>
        {me.name && <p className="sub" style={{ textAlign: 'left', marginTop: 0 }}>{me.name}{me.sub ? ` (${me.sub})` : ''}</p>}
        <button className="btn primary" type="button" onClick={onLogout} style={{ width: '100%', justifyContent: 'center' }}>
          Çıkış
        </button>
      </div>
    </div>
  )
}

/* Basit login kapısı: token yoksa gösterilir. Panel kütüphaneci içindir: L1001 / librarian123 */
function Login({ onSuccess }) {
  const [studentNumber, setStudentNumber] = useState('L1001')
  const [password, setPassword] = useState('librarian123')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const res = await api.login(studentNumber, password)
      setToken(res.token)
      onSuccess()
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <div className="brand">
          <span className="logo"><Icon.Book size={20} /></span>
          <span>LibraryApp<small>Yönetim Paneli</small></span>
        </div>
        <form onSubmit={submit}>
          <div className="field">
            <label htmlFor="studentNumber">Öğrenci numarası</label>
            <input id="studentNumber" value={studentNumber} onChange={(e) => setStudentNumber(e.target.value)} required autoFocus />
          </div>
          <div className="field">
            <label htmlFor="password">Parola</label>
            <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>
          {error && <div className="state error" style={{ padding: 0, textAlign: 'left' }}>{error}</div>}
          <button className="btn primary" type="submit" disabled={loading}>
            {loading ? <><span className="spinner" /> Giriş yapılıyor…</> : 'Giriş yap'}
          </button>
        </form>
        <p className="sub">Demo: L1001 / librarian123</p>
      </div>
    </div>
  )
}
