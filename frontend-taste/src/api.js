// JWT-farkında API katmanı. Token localStorage'da tutulur, her isteğe eklenir.
let token = localStorage.getItem('kitaplik_token') || null

export function setToken(t) {
  token = t
  if (t) localStorage.setItem('kitaplik_token', t)
  else localStorage.removeItem('kitaplik_token')
}
export function getToken() { return token }

// JWT payload'ını çöz (imza doğrulaması SUNUCUDA; burada sadece studentId/name okuyoruz).
export function decodeToken() {
  if (!token) return null
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return { studentId: payload.studentId, name: payload.name, sub: payload.sub }
  } catch {
    return null
  }
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(`/api${path}`, { ...options, headers })
  if (res.status === 401) {
    setToken(null)
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

export const api = {
  login: (studentNumber, password) =>
    request('/auth/login', { method: 'POST', body: JSON.stringify({ studentNumber, password }) }),

  // Ortak
  getBooks: () => request('/books'),
  getAuthors: () => request('/authors'),
  getCategories: () => request('/categories'),
  createAuthor: (a) => request('/authors', { method: 'POST', body: JSON.stringify(a) }),
  createCategory: (c) => request('/categories', { method: 'POST', body: JSON.stringify(c) }),
  createBook: (b) => request('/books', { method: 'POST', body: JSON.stringify(b) }),

  // Admin
  getStudents: () => request('/students'),
  getLoans: () => request('/loans'),
  getFines: () => request('/fines'),

  // Üye
  getMyLoans: (studentId) => request(`/loans/members/${studentId}`),
}
