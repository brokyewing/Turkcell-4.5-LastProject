import { useEffect, useState } from 'react'
import { api } from './api'

// ÜYE yüzeyi: öğrenci kendi kütüphanesini görür. Ferah, SaaS tarzı.
export default function UserView({ me }) {
  const [books, setBooks] = useState([])
  const [loans, setLoans] = useState([])
  const [copies, setCopies] = useState([])
  const [finesTotal, setFinesTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [msg, setMsg] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)

  // Ödünç alma formu: { book, copyId, due }
  const [borrow, setBorrow] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let alive = true
    ;(async () => {
      setLoading(true); setError(null)
      try {
        // GET /api/loans yalnız LIBRARIAN'a açık; öğrenci için kullanılmaz.
        // Müsaitlik, kitap DTO'sundaki availableCopies/totalCopies alanlarından okunur.
        const [b, l, c, f] = await Promise.all([
          api.getBooks(),
          me.studentId ? api.getMyLoans(me.studentId).catch(() => []) : Promise.resolve([]),
          api.getCopyBooks().catch(() => []),
          me.studentId ? api.getMyFinesTotal(me.studentId).catch(() => 0) : Promise.resolve(0),
        ])
        if (!alive) return
        setBooks(b || [])
        setLoans(l || [])
        setCopies(c || [])
        setFinesTotal(Number(f) || 0)
      } catch (e) {
        if (alive) setError(e.message)
      } finally {
        if (alive) setLoading(false)
      }
    })()
    return () => { alive = false }
  }, [me.studentId, reloadKey])

  const openLoans = loans.filter((l) => !l.isReturned)

  // Kitap DTO'su stok adedini verir (availableCopies). 0 ise hiç kopya yok.
  function availableCount(book) {
    return Math.max(0, Number(book.availableCopies) || 0)
  }

  // Kitabın kopyaları; öğrencinin kendi açık ödünçlerindeki kopyalar elenir.
  function copyCandidates(book) {
    const mine = new Set(openLoans.map((l) => l.copyBookId))
    return copies.filter((c) => c.bookId === book.id && !mine.has(c.id))
  }

  function startBorrow(book) {
    setMsg(null)
    const candidates = copyCandidates(book)
    if (availableCount(book) === 0 || candidates.length === 0) {
      setMsg({ err: true, text: `"${book.title}" için ödünç verilebilir kopya yok.` })
      return
    }
    setBorrow({ book, copyId: String(candidates[0].id), candidates, due: isoDate(addDays(new Date(), 14)) })
  }

  async function confirmBorrow(e) {
    e.preventDefault()
    if (!borrow) return
    // decodeToken çözülemediyse studentId NaN olur; backend'e bozuk istek gönderme.
    if (me.studentId == null || Number.isNaN(Number(me.studentId))) {
      setMsg({ err: true, text: 'Oturum bilgisi okunamadı. Lütfen tekrar giriş yapın.' })
      return
    }
    setBusy(true); setMsg(null)
    try {
      // Kopya bazlı doluluk öğrenciye açık bir uçtan okunamıyor; seçilen kopya
      // zaten ödünçteyse ("already loaned") sıradaki adayı dene.
      const ordered = [String(borrow.copyId), ...borrow.candidates.map((c) => String(c.id))]
        .filter((id, i, arr) => arr.indexOf(id) === i)
      let lastErr = null
      for (const copyId of ordered) {
        try {
          await api.createLoan({
            studentId: Number(me.studentId),
            copyBookId: Number(copyId),
            dueDate: `${borrow.due}T00:00:00`,
          })
          setMsg({ text: `"${borrow.book.title}" ödünç alındı. İade: ${fmt(borrow.due)}.` })
          setBorrow(null)
          setReloadKey((k) => k + 1)
          return
        } catch (e) {
          lastErr = e
          if (!/already loaned/i.test(e.message)) break
        }
      }
      setMsg({ err: true, text: lastErr ? lastErr.message : 'Ödünç alınamadı.' })
    } finally {
      setBusy(false)
    }
  }

  async function returnLoan(loanId) {
    setBusy(true); setMsg(null)
    try {
      await api.returnLoan(loanId)
      setMsg({ text: `Ödünç #${loanId} iade edildi.` })
      setReloadKey((k) => k + 1)
    } catch (e) {
      setMsg({ err: true, text: e.message })
    } finally {
      setBusy(false)
    }
  }

  const borrowFree = borrow ? borrow.candidates : []

  return (
    <div className="page">
      <div className="hero-u rise">
        <div>
          <h1>Merhaba, {me.name || 'okur'}.</h1>
          <p>Bugün ne okumak istersin?</p>
        </div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <div className="badge warn">Ödenmemiş ceza: {fmtMoney(finesTotal)} TL</div>
          <div className="badge warn">{openLoans.length} açık ödünç</div>
        </div>
      </div>

      {error && <div className="state err">Hata: {error}</div>}
      {msg && (
        <div className={`state ${msg.err ? 'err' : ''}`} role="status" style={msg.err ? undefined : { color: 'var(--ok)' }}>
          {msg.text}
        </div>
      )}

      {borrow && (
        <div className="panel rise" style={{ marginBottom: 22 }}>
          <div className="panel-h"><h2>Ödünç Al · {borrow.book.title}</h2></div>
          <div className="panel-b" style={{ padding: 16 }}>
            <form onSubmit={confirmBorrow} style={{ display: 'flex', gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
              <div style={{ flex: '1 1 220px' }}>
                <label htmlFor="copy-select">Kopya</label>
                <select id="copy-select" value={borrow.copyId} onChange={(e) => setBorrow({ ...borrow, copyId: e.target.value })}>
                  {borrowFree.map((c) => <option key={c.id} value={c.id}>Kopya #{c.id}</option>)}
                </select>
              </div>
              <div style={{ flex: '1 1 180px' }}>
                <label htmlFor="due-date">İade tarihi</label>
                <input id="due-date" type="date" value={borrow.due} min={isoDate(new Date())}
                  onChange={(e) => setBorrow({ ...borrow, due: e.target.value })} required />
              </div>
              <button className="btn primary" type="submit" disabled={busy}>
                {busy ? <><span className="spinner" /> Alınıyor…</> : 'Onayla'}
              </button>
              <button className="btn line" type="button" disabled={busy} onClick={() => setBorrow(null)}>Vazgeç</button>
            </form>
          </div>
        </div>
      )}

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
              <thead><tr><th>Ödünç #</th><th>Kopya</th><th>İade tarihi</th><th>Durum</th><th>İşlem</th></tr></thead>
              <tbody>
                {openLoans.map((l) => {
                  const overdue = l.dueDate && startOfDay(new Date(l.dueDate)) < startOfDay(new Date())
                  return (
                    <tr key={l.id}>
                      <td className="num">{l.id}</td>
                      <td className="num">{l.copyBookId}</td>
                      <td>{fmt(l.dueDate)}</td>
                      <td>{overdue
                        ? <span className="badge dan">Gecikmiş</span>
                        : <span className="badge ok">Zamanında</span>}</td>
                      <td>
                        <button className="btn line sm" disabled={busy} onClick={() => returnLoan(l.id)}>
                          İade Et
                        </button>
                      </td>
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
            {books.map((b, i) => {
              const freeCount = availableCount(b)
              const hasFree = freeCount > 0
              return (
                <div className="book rise" key={b.id} style={{ animationDelay: `${i * 0.03}s` }}>
                  <div className={`cover c${b.id % 6}`}><b>{b.title}</b></div>
                  <div className="meta">
                    <div className="t">{b.title}</div>
                    <div className="a">{b.authorName} {b.authorLastname}</div>
                    <div className="st">
                      {freeCount > 0
                        ? <span className="badge ok">{freeCount} müsait</span>
                        : <span className="badge dan">Tükendi</span>}
                    </div>
                    <button className="btn line sm" style={{ marginTop: 9, width: '100%', justifyContent: 'center' }}
                      disabled={!hasFree || busy}
                      onClick={() => startBorrow(b)}>
                      Ödünç Al
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </section>
    </div>
  )
}

function addDays(d, n) {
  const x = new Date(d)
  x.setDate(x.getDate() + n)
  return x
}

// Saat bileşenini sıfırla; gün bazında karşılaştırma için (backend ChronoUnit.DAYS ile hizalı).
function startOfDay(d) {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  return x
}

function isoDate(d) {
  const x = new Date(d)
  const p = (n) => String(n).padStart(2, '0')
  return `${x.getFullYear()}-${p(x.getMonth() + 1)}-${p(x.getDate())}`
}

function fmtMoney(n) {
  const v = Number(n) || 0
  return v.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmt(d) {
  if (!d) return '—'
  try { return new Date(d).toLocaleDateString('tr-TR') } catch { return d }
}
