// Tüm veriyi tek yerden yükleyip paylaşan basit "store".
// Böylece her ekran ayrı ayrı istek atmaz; ekleme sonrası tek reload() yeter.
import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { api } from './api'

const DataCtx = createContext(null)
export const useData = () => useContext(DataCtx)

export function DataProvider({ children }) {
  const [books, setBooks] = useState([])
  const [authors, setAuthors] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const reload = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      // Üç isteği paralel at — birbirini beklemesine gerek yok
      const [b, a, c] = await Promise.all([api.getBooks(), api.getAuthors(), api.getCategories()])
      setBooks(b); setAuthors(a); setCategories(c)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { reload() }, [reload])

  return (
    <DataCtx.Provider value={{ books, authors, categories, loading, error, reload }}>
      {children}
    </DataCtx.Provider>
  )
}
