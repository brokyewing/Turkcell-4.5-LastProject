// Tüm istekler /api ile başlar ve Vite proxy tarafından gateway'e (8080)
// iletilir. Gateway de Eureka'ya sorup doğru mikroservise yönlendirir.
// Yani frontend hangi servisin nerede olduğunu BİLMEZ — sadece gateway'i bilir.

async function request(path, options = {}) {
  const res = await fetch(`/api${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
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
  // Kitaplar
  getBooks: () => request('/books'),
  createBook: (book) => request('/books', { method: 'POST', body: JSON.stringify(book) }),

  // Yazarlar
  getAuthors: () => request('/authors'),
  createAuthor: (author) => request('/authors', { method: 'POST', body: JSON.stringify(author) }),

  // Kategoriler
  getCategories: () => request('/categories'),
  createCategory: (category) => request('/categories', { method: 'POST', body: JSON.stringify(category) }),
}
