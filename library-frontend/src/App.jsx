import { useState, useEffect } from 'react'
import { DataProvider, useData } from './data'
import { ToastProvider } from './ui'
import { Icon } from './icons'
import { Dashboard, BooksView, AuthorsView, CategoriesView } from './views'

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

function Shell() {
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
          <button className="btn icon" onClick={toggleTheme} aria-label={theme === 'dark' ? 'Açık moda geç' : 'Koyu moda geç'} title="Tema değiştir">
            {theme === 'dark' ? <Icon.Sun size={18} /> : <Icon.Moon size={18} />}
          </button>
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
  return (
    <ToastProvider>
      <DataProvider>
        <Shell />
      </DataProvider>
    </ToastProvider>
  )
}
