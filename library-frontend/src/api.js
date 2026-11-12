// Tüm istekler API köküyle başlar ve Vite proxy tarafından gateway'e (18090)
// iletilir. Gateway de Eureka'ya sorup doğru mikroservise yönlendirir.
// API kökü: üretimde '/api' (tek origin), alt yol demosunda VITE_API_BASE ile ezilir.
// JWT token localStorage'da tutulur; her korumalı isteğe Authorization eklenir.

const API_BASE = (import.meta.env.VITE_API_BASE || '/api').replace(/\/$/, '')

const TOKEN_KEY = 'library_token'

let token = localStorage.getItem(TOKEN_KEY) || null

export function setToken(t) {
  token = t
  if (t) localStorage.setItem(TOKEN_KEY, t)
  else localStorage.removeItem(TOKEN_KEY)
}

export function getToken() {
  return token || localStorage.getItem(TOKEN_KEY)
}

export function isAuthed() {
  return !!getToken()
}

// JWT payload çözümü tek kaynakta: ./utils/jwt (decodeToken(token)).

async function request(path, options = {}) {
  const current = getToken()
  if (!current && path !== '/auth/login') {
    throw new Error('Giriş yapmanız gerekiyor.')
  }

  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  if (current) headers.Authorization = `Bearer ${current}`

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers })

  if (res.status === 401) {
    setToken(null)
    // Token temizlendi; App.jsx dinleyip oturumu kapatıyor (login ekranına dönüş).
    window.dispatchEvent(new Event('auth:logout'))
    throw new Error('Oturum geçersiz veya yetkiniz yok. Lütfen tekrar giriş yapın.')
  }
  if (!res.ok) {
    // Backend'in GlobalExceptionHandler'ı hata gövdesi döndürüyor; onu okumaya çalış.
    let message = `HTTP ${res.status}`
    try {
      const body = await res.json()
      message = body.message || body.error || JSON.stringify(body)
    } catch {
      // gövde yoksa varsayılan mesaj kalsın
    }
    throw new Error(message)
  }
  // 204 No Content gibi durumlarda gövde olmayabilir
  const text = await res.text()
  return text ? JSON.parse(text) : null
}

export const api = {
  // Kimlik
  login: (studentNumber, password) =>
    request('/auth/login', { method: 'POST', body: JSON.stringify({ studentNumber, password }) }),

  // Kitaplar
  getBooks: () => request('/books?size=500'),
  createBook: (book) => request('/books', { method: 'POST', body: JSON.stringify(book) }),

  // Yazarlar
  getAuthors: () => request('/authors'),
  createAuthor: (author) => request('/authors', { method: 'POST', body: JSON.stringify(author) }),

  // Kategoriler
  getCategories: () => request('/categories'),
  createCategory: (category) => request('/categories', { method: 'POST', body: JSON.stringify(category) }),
}
