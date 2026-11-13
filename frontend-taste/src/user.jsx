import { useEffect, useMemo, useState } from 'react'
import { api } from './api'
import {
  Cover,
  Modal,
  Tag,
  addDays,
  cx,
  daysLeft,
  fmtDate,
  fmtDateLong,
  isoDate,
  money,
  todayLong,
} from './ui.jsx'

const LOAN_DAYS = 14

export default function UserView({ me, tab = 'discover', onNavigate }) {
  const [books, setBooks] = useState([])
  const [loans, setLoans] = useState([])
  const [copies, setCopies] = useState([])
  const [finesTotal, setFinesTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [toast, setToast] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [busy, setBusy] = useState(false)

  // Keşfet filtreleri
  const [q, setQ] = useState('')
  const [cat, setCat] = useState('all')

  // Ödünç alma fişi: { book, copyId, due, candidates }
  const [borrow, setBorrow] = useState(null)

  useEffect(() => {
    let alive = true
    ;(async () => {
      setLoading(true)
      setError(null)
      try {
        // GET /api/loans yalnız LIBRARIAN'a açık; müsaitlik kitap DTO'sundan okunur.
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
    return () => {
      alive = false
    }
  }, [me.studentId, reloadKey])

  useEffect(() => {
    if (!toast) return undefined
    const t = setTimeout(() => setToast(null), 4200)
    return () => clearTimeout(t)
  }, [toast])

  const openLoans = loans.filter((l) => !l.isReturned)
  const pastLoans = loans.filter((l) => l.isReturned)

  function availableCount(book) {
    return Math.max(0, Number(book.availableCopies) || 0)
  }

  function copyCandidates(book) {
    const mine = new Set(openLoans.map((l) => l.copyBookId))
    return copies.filter((c) => c.bookId === book.id && !mine.has(c.id))
  }

  function bookForLoan(loan) {
    const copy = copies.find((c) => c.id === loan.copyBookId)
    const book = copy ? books.find((b) => b.id === copy.bookId) : null
    return book || { title: loan.bookTitle || `Kopya #${loan.copyBookId}` }
  }

  function startBorrow(book) {
    if (availableCount(book) === 0) {
      setToast({ err: true, text: `"${book.title}" için ödünç verilebilir kopya yok.` })
      return
    }
    const candidates = copyCandidates(book)
    if (candidates.length === 0) {
      setToast({ err: true, text: 'Kopya listesi yüklenemedi. Sayfayı yenileyip tekrar deneyin.' })
      return
    }
    setBorrow({
      book,
      copyId: String(candidates[0].id),
      candidates,
      due: isoDate(addDays(new Date(), LOAN_DAYS)),
    })
  }

  async function confirmBorrow(e) {
    e.preventDefault()
    if (!borrow) return
    if (me.studentId == null || Number.isNaN(Number(me.studentId))) {
      setToast({ err: true, text: 'Oturum bilgisi okunamadı. Lütfen tekrar giriş yapın.' })
      return
    }
    setBusy(true)
    try {
      // Seçilen kopya dolmuşsa ("already loaned") sıradaki adayı dene.
      const ordered = [String(borrow.copyId), ...borrow.candidates.map((c) => String(c.id))].filter(
        (id, i, arr) => arr.indexOf(id) === i,
      )
      let lastErr = null
      for (const copyId of ordered) {
        try {
          await api.createLoan({
            studentId: Number(me.studentId),
            copyBookId: Number(copyId),
            dueDate: `${borrow.due}T00:00:00`,
          })
          setToast({
            text: `"${borrow.book.title}" ödünç alındı · Kopya #${copyId} · İade ${fmtDate(borrow.due)}`,
          })
          setBorrow(null)
          setReloadKey((k) => k + 1)
          return
        } catch (err) {
          lastErr = err
          if (!/already loaned/i.test(err.message)) break
        }
      }
      setToast({ err: true, text: lastErr ? lastErr.message : 'Ödünç alınamadı.' })
    } finally {
      setBusy(false)
    }
  }

  async function returnLoan(loanId) {
    setBusy(true)
    try {
      await api.returnLoan(loanId)
      setToast({ text: `Ödünç #${loanId} iade edildi.` })
      setReloadKey((k) => k + 1)
    } catch (e) {
      setToast({ err: true, text: e.message })
    } finally {
      setBusy(false)
    }
  }

  const categories = useMemo(() => {
    const map = new Map()
    books.forEach((b) => {
      const name = b.categoryName || 'Diğer'
      map.set(name, (map.get(name) || 0) + 1)
    })
    return [...map.entries()].sort((a, b) => b[1] - a[1])
  }, [books])

  const filtered = useMemo(() => {
    const needle = q.trim().toLocaleLowerCase('tr')
    return books.filter((b) => {
      if (cat !== 'all' && (b.categoryName || 'Diğer') !== cat) return false
      if (!needle) return true
      const hay = `${b.title} ${b.authorName} ${b.authorLastname} ${b.isbn}`.toLocaleLowerCase('tr')
      return hay.includes(needle)
    })
  }, [books, q, cat])

  const nextDue = openLoans
    .map((l) => l.dueDate)
    .filter(Boolean)
    .sort((a, b) => new Date(a) - new Date(b))[0]

  return (
    <main className="page">
      {error && (
        <div className="form-err" role="alert" style={{ marginBottom: 20 }}>
          <span>{error}</span>
        </div>
      )}

      {tab === 'loans' ? (
        <LoansSurface
          loading={loading}
          openLoans={openLoans}
          pastLoans={pastLoans}
          bookForLoan={bookForLoan}
          onReturn={returnLoan}
          busy={busy}
          onBrowse={() => onNavigate && onNavigate('discover')}
        />
      ) : (
        <>
          <section className="hero">
            <div className="hero-main rise">
              <span className="eyebrow">{todayLong()}</span>
              <h1 className="h1">
                Merhaba, <span className="em">{me.name || 'okur'}.</span>
              </h1>
              <p className="lede">Bugün ne okumak istersin? Raf seni bekliyor.</p>

              <div className="slips">
                <div className="slip">
                  <div className="k">Açık ödünç</div>
                  <div className="v tnum">
                    {loading ? '—' : openLoans.length}
                    <small>kitap</small>
                  </div>
                </div>
                <div className="slip">
                  <div className="k">En yakın iade</div>
                  <div className="v tnum" style={{ fontSize: 19, paddingTop: 4 }}>
                    {loading ? '—' : fmtDate(nextDue)}
                  </div>
                </div>
                <div className={cx('slip', finesTotal > 0 && 'hot')}>
                  <div className="k">Ödenmemiş ceza</div>
                  <div className="v tnum">
                    {loading ? '—' : money(finesTotal)}
                    <small>₺</small>
                  </div>
                </div>
              </div>
            </div>

            <aside className="collection rise" style={{ animationDelay: '0.06s' }}>
              <div className="collection-h">
                <span className="mono">KOLEKSİYON</span>
                <span className="mono" style={{ letterSpacing: '0.06em' }}>
                  {books.length} kitap
                </span>
              </div>
              {categories.length === 0 ? (
                <div className="muted" style={{ fontSize: 13 }}>
                  Katalog henüz boş.
                </div>
              ) : (
                categories.map(([name, count]) => (
                  <div className="coll-row" key={name}>
                    <div className="coll-top">
                      <span>{name}</span>
                      <span>{count}</span>
                    </div>
                    <div className="bar">
                      <i style={{ width: `${Math.max(12, (count / books.length) * 100)}%` }} />
                    </div>
                  </div>
                ))
              )}
            </aside>
          </section>

          <section>
            <div className="catalog-head">
              <div>
                <span className="eyebrow">KATALOG</span>
                <h2 className="h2" style={{ marginTop: 10 }}>
                  Keşfet
                </h2>
              </div>
              <span className="count">
                {loading ? 'taranıyor…' : `${filtered.length} / ${books.length} kitap`}
              </span>
            </div>

            <div className="filters">
              <div className="chips">
                <button className={cx('chip', cat === 'all' && 'on')} onClick={() => setCat('all')}>
                  Tümü
                </button>
                {categories.map(([name]) => (
                  <button key={name} className={cx('chip', cat === name && 'on')} onClick={() => setCat(name)}>
                    {name}
                  </button>
                ))}
              </div>
              <div className="search">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  <circle cx="11" cy="11" r="7" />
                  <path d="m20 20-3.5-3.5" />
                </svg>
                <input
                  value={q}
                  onChange={(e) => setQ(e.target.value)}
                  placeholder="Başlık, yazar veya ISBN"
                  aria-label="Katalogda ara"
                />
              </div>
            </div>

            {loading ? (
              <div className="state">
                <span className="spinner" /> Katalog taranıyor…
              </div>
            ) : books.length === 0 ? (
              <div className="slip-empty">
                <div className="t">Raf henüz boş</div>
                <p>Kütüphaneci kitap eklediğinde burada görünecek.</p>
              </div>
            ) : filtered.length === 0 ? (
              <div className="slip-empty">
                <div className="t">Eşleşen kitap yok</div>
                <p>Aramanı daraltmış olabilirsin. Filtreleri sıfırlayıp tekrar dene.</p>
                <button
                  className="btn line sm"
                  onClick={() => {
                    setQ('')
                    setCat('all')
                  }}
                >
                  Filtreleri temizle
                </button>
              </div>
            ) : (
              <div className="books">
                {filtered.map((b, i) => {
                  const free = availableCount(b)
                  const year = b.publicationDate ? String(b.publicationDate).slice(0, 4) : ''
                  return (
                    <article className="bcard rise" key={b.id} style={{ animationDelay: `${Math.min(i, 8) * 0.035}s` }}>
                      <Cover book={b} />
                      <div className="pad">
                        <h3 className="t">{b.title}</h3>
                        <div className="a">
                          {b.authorName} {b.authorLastname}
                          {year ? ` · ${year}` : ''}
                        </div>
                        <div className="row">
                          <span className={cx('stock', free === 0 && 'out')}>
                            <i />
                            {free === 0 ? 'Tükendi' : `${free} müsait`}
                          </span>
                          <span className="mono" style={{ letterSpacing: '0.04em', textTransform: 'none' }}>
                            {b.categoryName || '—'}
                          </span>
                        </div>
                        <button className="go" disabled={free === 0 || busy} onClick={() => startBorrow(b)}>
                          {free === 0 ? 'Şu an ödünçte' : 'Ödünç al'}
                          {free > 0 && (
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                              <path d="M5 12h14M13 6l6 6-6 6" />
                            </svg>
                          )}
                        </button>
                      </div>
                    </article>
                  )
                })}
              </div>
            )}
          </section>
        </>
      )}

      <BorrowSheet
        borrow={borrow}
        busy={busy}
        onClose={() => !busy && setBorrow(null)}
        onSubmit={confirmBorrow}
        onChangeCopy={(copyId) => setBorrow((s) => (s ? { ...s, copyId } : s))}
        onChangeDue={(due) => setBorrow((s) => (s ? { ...s, due } : s))}
      />

      {toast && (
        <div className="toasts">
          <div className={cx('toast', toast.err && 'err')} role="status">
            <span className="txt">{toast.text}</span>
          </div>
        </div>
      )}
    </main>
  )
}

/* -------------------------------------------------------------------------- */

function LoansSurface({ loading, openLoans, pastLoans, bookForLoan, onReturn, busy, onBrowse }) {
  return (
    <>
      <div className="page-head">
        <div>
          <span className="eyebrow">ÖDÜNÇ DEFTERİ</span>
          <h1 className="h1" style={{ marginTop: 12 }}>
            Kitaplarım
          </h1>
          <p className="lede" style={{ marginTop: 8 }}>
            Üzerindeki kitaplar, iade tarihleri ve geçmişin.
          </p>
        </div>
        <span className="meta">{loading ? '—' : `${openLoans.length} açık · ${pastLoans.length} iade`}</span>
      </div>

      {loading ? (
        <div className="state">
          <span className="spinner" /> Defter açılıyor…
        </div>
      ) : openLoans.length === 0 ? (
        <div className="slip-empty">
          <div className="t">Şu an ödünçte kitabın yok</div>
          <p>Katalogdan bir kitap seç, 14 günlüğüne yanına al.</p>
          <button className="btn primary sm" onClick={onBrowse}>
            Kataloğa göz at
          </button>
        </div>
      ) : (
        <div className="loans">
          {openLoans.map((l) => {
            const book = bookForLoan(l)
            const left = daysLeft(l.dueDate)
            const late = left !== null && left < 0
            const soon = left !== null && left >= 0 && left <= 3
            const total = LOAN_DAYS * 86400000
            const elapsed = l.loanDate ? startOf(l.loanDate) : null
            const remain = l.dueDate ? startOf(l.dueDate) : null
            let pct = 38
            if (elapsed != null && remain != null) {
              const span = remain - elapsed
              const used = Date.now() - elapsed
              pct = span > 0 ? Math.min(100, Math.max(4, (used / span) * 100)) : 100
            } else if (remain != null) {
              pct = Math.min(100, Math.max(4, ((total - (remain - Date.now())) / total) * 100))
            }
            return (
              <article className={cx('loan', late && 'late', soon && 'warn')} key={l.id}>
                <Cover book={book} />
                <div className="loan-main">
                  <div className="t">{book.title}</div>
                  <div className="a">
                    {book.authorName ? `${book.authorName} ${book.authorLastname}` : `Kopya #${l.copyBookId}`}
                  </div>
                  <div className="loan-meta">
                    <span className="k">İade</span>
                    <span className="v tnum">{fmtDateLong(l.dueDate)}</span>
                    {left !== null && (
                      <Tag tone={late ? 'dan' : soon ? 'warn' : 'ok'}>{leftLabel(left)}</Tag>
                    )}
                  </div>
                  <div className="loan-meta" style={{ marginTop: 10 }}>
                    <div className="meter">
                      <div className="bar">
                        <i style={{ width: `${pct}%` }} />
                      </div>
                    </div>
                    <span className="k">Kopya #{l.copyBookId}</span>
                  </div>
                </div>
                <div className="loan-actions">
                  <button className="btn line sm" disabled={busy} onClick={() => onReturn(l.id)}>
                    İade et
                  </button>
                  <span className="mono" style={{ letterSpacing: '0.06em', textTransform: 'none' }}>
                    #{l.id}
                  </span>
                </div>
              </article>
            )
          })}
        </div>
      )}

      {!loading && pastLoans.length > 0 && (
        <>
          <div className="doublerule" />
          <div className="panel">
            <div className="panel-h">
              <h2>Geçmiş</h2>
              <span className="mono">{pastLoans.length} kayıt</span>
            </div>
            <div className="panel-b">
              <div className="tw">
                <table className="table">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Kitap</th>
                      <th>Veriliş</th>
                      <th>İade</th>
                      <th>Durum</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[...pastLoans].reverse().map((l) => {
                      const book = bookForLoan(l)
                      const late = l.returnDate && l.dueDate && new Date(l.returnDate) > new Date(l.dueDate)
                      return (
                        <tr key={l.id}>
                          <td className="num">{l.id}</td>
                          <td className="strong">{book.title}</td>
                          <td className="num">{fmtDate(l.loanDate)}</td>
                          <td className="num">{fmtDate(l.returnDate)}</td>
                          <td>{late ? <Tag tone="dan">Geç iade</Tag> : <Tag tone="ok">Zamanında</Tag>}</td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </>
      )}
    </>
  )
}

function BorrowSheet({ borrow, busy, onClose, onSubmit, onChangeCopy, onChangeDue }) {
  const open = Boolean(borrow)
  return (
    <Modal open={open} onClose={onClose} labelledBy="borrow-title">
      {borrow && (
        <>
          <div className="sheet-head">
            <span className="mono">ÖDÜNÇ FİŞİ</span>
            <button className="icon-btn" onClick={onClose} aria-label="Kapat" style={{ width: 32, height: 32 }}>
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                <path d="M6 6l12 12M18 6L6 18" />
              </svg>
            </button>
          </div>
          <div className="sheet-body">
            <div className="sheet-book">
              <Cover book={borrow.book} />
              <div>
                <h3 className="h3" id="borrow-title">
                  {borrow.book.title}
                </h3>
                <div className="muted">
                  {borrow.book.authorName} {borrow.book.authorLastname}
                  {borrow.book.publicationDate ? ` · ${String(borrow.book.publicationDate).slice(0, 4)}` : ''}
                </div>
                <div style={{ marginTop: 9 }}>
                  <Tag tone="ok">{borrow.book.categoryName || 'Katalog'}</Tag>
                </div>
              </div>
            </div>

            <form onSubmit={onSubmit}>
              <div className="field">
                <label className="field-label" htmlFor="copy-select">
                  Kopya
                </label>
                <select id="copy-select" value={borrow.copyId} onChange={(e) => onChangeCopy(e.target.value)}>
                  {borrow.candidates.map((c) => (
                    <option key={c.id} value={c.id}>
                      Kopya #{c.id}
                    </option>
                  ))}
                </select>
              </div>

              <div className="field">
                <label className="field-label" htmlFor="due-date">
                  İade tarihi
                </label>
                <input
                  id="due-date"
                  type="date"
                  value={borrow.due}
                  min={isoDate(new Date())}
                  onChange={(e) => onChangeDue(e.target.value)}
                  required
                />
              </div>

              <div className="note">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" style={{ flex: '0 0 15px', marginTop: 1 }}>
                  <circle cx="12" cy="12" r="9" />
                  <path d="M12 11v5M12 8v.01" />
                </svg>
                <span>Standart ödünç {LOAN_DAYS} gündür. Gecikmede gün başına 5 ₺ ceza işler.</span>
              </div>

              <div className="actions">
                <button className="btn primary" type="submit" disabled={busy}>
                  {busy ? (
                    <>
                      <span className="spinner" /> Alınıyor…
                    </>
                  ) : (
                    'Ödünç al'
                  )}
                </button>
                <button className="btn line" type="button" disabled={busy} onClick={onClose}>
                  Vazgeç
                </button>
              </div>
            </form>
          </div>
        </>
      )}
    </Modal>
  )
}

function leftLabel(left) {
  if (left < 0) return `${-left} gün gecikti`
  if (left === 0) return 'Bugün son gün'
  if (left === 1) return 'Yarın son gün'
  return `${left} gün kaldı`
}

function startOf(d) {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  return x.getTime()
}
