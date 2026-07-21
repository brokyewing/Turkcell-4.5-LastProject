import { useEffect, useState } from 'react'
import { api } from './api'

// ÜYE yüzeyi: öğrenci kendi kütüphanesini görür. Ferah, SaaS tarzı.
export default function UserView({ me }) {
  const [books, setBooks] = useState([])
  const [loans, setLoans] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let alive = true
    ;(async () => {
      setLoading(true); setError(null)
      try {
        const [b, l] = await Promise.all([
          api.getBooks(),
          me.studentId ? api.getMyLoans(me.studentId).catch(() => []) : Promise.resolve([]),
        ])
        if (alive) { setBooks(b); setLoans(l || []) }
      } catch (e) {
        if (alive) setError(e.message)
      } finally {
        if (alive) setLoading(false)
      }
    })()
    return () => { alive = false }
  }, [me.studentId])

  const openLoans = loans.filter((l) => !l.isReturned)

  return (
    <div className="page">
      <div className="hero-u rise">
        <div>
          <h1>Merhaba, {me.name || 'okur'}.</h1>
          <p>Bugün ne okumak istersin?</p>
        </div>
        <div className="badge warn">{openLoans.length} açık ödünç</div>
      </div>

      {error && <div className="state err">Hata: {error}</div>}

      {/* Kitaplarım */}
      <section style={{ marginBottom: 30 }}>
        <div className="sec-title">Kitaplarım</div>
        {loading ? (
          <div className="state"><span className="spinner" /> Yükleniyor…</div>
        ) : openLoans.length === 0 ? (
          <div className="panel"><div className="state">Şu an ödünçte kitabın yok.</div></div>
        ) : (
          <div className="panel"><div className="panel-b"><div className="tw">
            <table>
              <thead><tr><th>Ödünç #</th><th>İade tarihi</th><th>Durum</th></tr></thead>
              <tbody>
                {openLoans.map((l) => {
                  const overdue = l.dueDate && new Date(l.dueDate) < new Date()
                  return (
                    <tr key={l.id}>
                      <td className="num">{l.id}</td>
                      <td>{fmt(l.dueDate)}</td>
                      <td>{overdue
                        ? <span className="badge dan">Gecikmiş</span>
                        : <span className="badge ok">Zamanında</span>}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div></div></div>
        )}
      </section>

      {/* Keşfet */}
      <section>
        <div className="sec-title">Keşfet</div>
        {loading ? (
          <div className="state"><span className="spinner" /> Yükleniyor…</div>
        ) : books.length === 0 ? (
          <div className="panel"><div className="state">Katalogda henüz kitap yok.</div></div>
        ) : (
          <div className="books">
            {books.map((b, i) => (
              <div className="book rise" key={b.id} style={{ animationDelay: `${i * 0.03}s` }}>
                <div className={`cover c${b.id % 6}`}><b>{b.title}</b></div>
                <div className="meta">
                  <div className="t">{b.title}</div>
                  <div className="a">{b.authorName} {b.authorLastname}</div>
                  <div className="st">
                    {b.availableCopies > 0
                      ? <span className="badge ok">{b.availableCopies} müsait</span>
                      : <span className="badge dan">Tükendi</span>}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

function fmt(d) {
  if (!d) return '—'
  try { return new Date(d).toLocaleDateString('tr-TR') } catch { return d }
}
