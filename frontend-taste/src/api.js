// JWT-farkında API katmanı. Token localStorage'da tutulur, her isteğe eklenir.
let token = localStorage.getItem('kitaplik_token') || null

export function setToken(t) {
  token = t
  if (t) localStorage.setItem('kitaplik_token', t)
  else localStorage.removeItem('kitaplik_token')
}
export function getToken() { return token }

// JWT payload çözümü tek kaynakta: ./utils/jwt (decodeToken(token)).

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(`/api${path}`, { ...options, headers })
  if (res.status === 401) {
    setToken(null)
    // Token temizlendi; App.jsx dinleyip oturumu kapatıyor (login ekranına dönüş).
    window.dispatchEvent(new Event('auth:logout'))
    throw new Error('Oturum geçersiz. Lütfen tekrar giriş yapın.')
  }
  if (!res.ok) {
    let message = `HTTP ${res.status}`
    try { const b = await res.json(); message = b.message || message } catch {}
    throw new Error(message)
  }
  const text = await res.text()
  return text ? JSON.parse(text) : null
}

// /api/books Spring Page döner ({content:[...]}) — tek yerde diziye indir.
function toList(data) {
  if (Array.isArray(data)) return data
  return data?.content ?? []
}

export const api = {
  login: (studentNumber, password) =>
    request('/auth/login', { method: 'POST', body: JSON.stringify({ studentNumber, password }) }),

  // Ortak
  getBooks: async () => toList(await request('/books?size=500')),
  getAuthors: () => request('/authors'),
  getCategories: () => request('/categories'),
  getCopyBooks: () => request('/copybooks'),
  createAuthor: (a) => request('/authors', { method: 'POST', body: JSON.stringify(a) }),
  createCategory: (c) => request('/categories', { method: 'POST', body: JSON.stringify(c) }),
  createBook: (b) => request('/books', { method: 'POST', body: JSON.stringify(b) }),

  // Admin
  getStudents: () => request('/students'),
  getLoans: () => request('/loans'),
  getFines: () => request('/fines'),

  // Üye
  getMyLoans: (studentId) => request(`/loans/members/${studentId}`),
  createLoan: (body) => request('/loans', { method: 'POST', body: JSON.stringify(body) }),
  returnLoan: (loanId) => request('/loans/return', { method: 'POST', body: JSON.stringify({ loanId }) }),
  getMyFinesTotal: (studentId) => request(`/fines/students/${studentId}/total`),
}
