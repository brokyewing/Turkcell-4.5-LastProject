import { useEffect, useState } from 'react'
import { api } from './api'
import { Tag, cx, fmtDate, money, todayLong } from './ui.jsx'

export default function AdminView({ me }) {
  const [data, setData] = useState({ books: [], students: [], loans: [], fines: [], authors: [], categories: [], copies: [] })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [toast, setToast] = useState(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [books, students, loans, fines, authors, categories, copies] = await Promise.all([
        api.getBooks(),
        api.getStudents().catch(() => []),
        api.getLoans().catch(() => []),
        api.getFines().catch(() => []),
        api.getAuthors(),
        api.getCategories(),
        api.getCopyBooks().catch(() => []),
      ])
      setData({
        books,
        students: students || [],
        loans: loans || [],
        fines: fines || [],
        authors,
        categories,
        copies: copies || [],
      })
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!toast) return undefined
    const t = setTimeout(() => setToast(null), 3400)
    return () => clearTimeout(t)
  }, [toast])

  const openLoans = data.loans.filter((l) => !l.isReturned)
  const overdue = openLoans.filter((l) => l.dueDate && new Date(l.dueDate) < new Date())
  const unpaid = data.fines.filter((f) => !f.isPaid)
  const unpaidTotal = unpaid.reduce((s, f) => s + (Number(f.amount) || 0), 0)
  const totalCopies = data.books.reduce((s, b) => s + (Number(b.totalCopies) || 0), 0)
  const freeCopies = data.books.reduce((s, b) => s + (Number(b.availableCopies) || 0), 0)

  function notify(text, err = false) {
    setToast({ text, err })
  }

  // LoanResponseDto her zaman studentName/bookTitle doldurmuyor; eldeki listelerden çözüyoruz.
  function studentLabel(studentId, fallbackName, fallbackLastname) {
    if (fallbackName) return `${fallbackName} ${fallbackLastname || ''}`.trim()
    const s = data.students.find((x) => x.id === studentId)
    return s ? `${s.name} ${s.lastname}` : `Üye #${studentId}`
  }

  function bookLabel(copyBookId, fallbackTitle) {
    if (fallbackTitle) return fallbackTitle
    const c = data.copies.find((x) => x.id === copyBookId)
    const b = c ? data.books.find((x) => x.id === c.bookId) : null
    return b ? b.title : `Kopya #${copyBookId}`
  }

  return (
    <main className="page">
      <div className="page-head">
        <div>
          <span className="eyebrow">KÜTÜPHANE MASASI</span>
          <h1 className="h1" style={{ marginTop: 12 }}>
            Günlük özet
          </h1>
          <p className="lede" style={{ marginTop: 8 }}>
            {todayLong()} · {me?.name ? `${me.name}, ` : ''}masa senin.
          </p>
        </div>
        <button className="btn line" onClick={load} disabled={loading}>
          {loading ? <span className="spinner" /> : (
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 12a9 9 0 1 1-2.6-6.4" />
              <path d="M21 4v5h-5" />
            </svg>
          )}
          Yenile
        </button>
      </div>

      {error && (
        <div className="form-err" role="alert" style={{ marginBottom: 20 }}>
          <span>{error}</span>
        </div>
      )}

      <div className="stats">
        <div className="stat rise">
          <div className="k">Kitap</div>
          <div className="v">{loading ? '—' : data.books.length}</div>
          <div className="sub">
            {loading ? ' ' : `${freeCopies}/${totalCopies} kopya rafta`}
          </div>
        </div>
        <div className="stat pine rise" style={{ animationDelay: '0.04s' }}>
          <div className="k">Üye</div>
          <div className="v">{loading ? '—' : data.students.length}</div>
          <div className="sub">kayıtlı okur</div>
        </div>
        <div className="stat rise" style={{ animationDelay: '0.08s' }}>
          <div className="k">Açık ödünç</div>
          <div className="v">{loading ? '—' : openLoans.length}</div>
          <div className="sub">{overdue.length > 0 ? `${overdue.length} gecikmiş` : 'gecikme yok'}</div>
        </div>
        <div className={cx('stat', unpaidTotal > 0 && 'hot', 'rise')} style={{ animationDelay: '0.12s' }}>
          <div className="k">Ödenmemiş ceza</div>
          <div className="v">
            {loading ? '—' : money(unpaidTotal)}
            <small>₺</small>
          </div>
          <div className="sub">{unpaid.length} açık kayıt</div>
        </div>
      </div>

      <div className="desk-grid">
        <div className="desk-col">
          <Panel title="Ödünçler" meta={`${data.loans.length} kayıt`}>
            {data.loans.length === 0 ? (
              <Empty msg="Henüz ödünç kaydı yok." />
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Üye</th>
                    <th>Kitap</th>
                    <th>İade</th>
                    <th>Durum</th>
                  </tr>
                </thead>
                <tbody>
                  {[...data.loans]
                    .reverse()
                    .slice(0, 8)
                    .map((l) => {
                      const late = !l.isReturned && l.dueDate && new Date(l.dueDate) < new Date()
                      return (
                        <tr key={l.id}>
                          <td className="num">{l.id}</td>
                          <td className="strong">
                            {studentLabel(l.studentId, l.studentName, l.studentLastname)}
                          </td>
                          <td>{bookLabel(l.copyBookId, l.bookTitle)}</td>
                          <td className="num">{fmtDate(l.dueDate)}</td>
                          <td>
                            {l.isReturned ? (
                              <Tag tone="ink">İade edildi</Tag>
                            ) : late ? (
                              <Tag tone="dan">Gecikmiş</Tag>
                            ) : (
                              <Tag tone="warn">Ödünçte</Tag>
                            )}
                          </td>
                        </tr>
                      )
                    })}
                </tbody>
              </table>
            )}
          </Panel>

          <Panel title="Cezalar" meta={`${data.fines.length} kayıt`}>
            {data.fines.length === 0 ? (
              <Empty msg="Ceza yok — herkes zamanında iade ediyor." />
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Üye</th>
                    <th>Tutar</th>
                    <th>Tür</th>
                    <th>Durum</th>
                  </tr>
                </thead>
                <tbody>
                  {data.fines.slice(0, 8).map((f) => (
                    <tr key={f.id}>
                      <td className="num">{f.id}</td>
                      <td>{studentLabel(f.studentId)}</td>
                      <td className="strong tnum">{money(f.amount)} ₺</td>
                      <td>
                        <Tag tone="warn">{f.fineType}</Tag>
                      </td>
                      <td>{f.isPaid ? <Tag tone="ok">Ödendi</Tag> : <Tag tone="dan">Bekliyor</Tag>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Panel>

          <Panel title="Katalog" meta={`${data.books.length} kitap`}>
            {data.books.length === 0 ? (
              <Empty msg="Katalog boş." />
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>Kitap</th>
                    <th>Kategori</th>
                    <th>Yıl</th>
                    <th>Rafta</th>
                  </tr>
                </thead>
                <tbody>
                  {data.books.map((b) => {
                    const total = Number(b.totalCopies) || 0
                    const free = Number(b.availableCopies) || 0
                    return (
                      <tr key={b.id}>
                        <td className="strong">{b.title}</td>
                        <td>{b.categoryName || '—'}</td>
                        <td className="num">{b.publicationDate ? String(b.publicationDate).slice(0, 4) : '—'}</td>
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 9, minWidth: 120 }}>
                            <div className="bar" style={{ flex: 1 }}>
                              <i style={{ width: `${total ? (free / total) * 100 : 0}%` }} />
                            </div>
                            <span className="num" style={{ fontFamily: 'var(--mono)', fontSize: 11.5 }}>
                              {free}/{total}
                            </span>
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            )}
          </Panel>
        </div>

        <div className="desk-col">
          <AddBook
            authors={data.authors}
            categories={data.categories}
            onAdded={() => {
              load()
              notify('Kitap kataloğa eklendi.')
            }}
            onError={(m) => notify(m, true)}
          />

          <div className="panel">
            <div className="panel-h">
              <h2>Üyeler</h2>
              <span className="mono">{data.students.length}</span>
            </div>
            <div className="people">
              {data.students.length === 0 ? (
                <Empty msg="Kayıtlı üye yok." />
              ) : (
                data.students.map((s) => (
                  <div className="person" key={s.id}>
                    <span className="av">{(s.name || '?')[0]}</span>
                    <span className="who">
                      <b>
                        {s.name} {s.lastname}
                      </b>
                      <span>
                        {s.studentNumber} · {s.departmentName || 'Bölüm yok'}
                      </span>
                    </span>
                    <Tag tone="ink">{s.membershipLevel}</Tag>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

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

function Panel({ title, meta, children }) {
  return (
    <section className="panel">
      <div className="panel-h">
        <h2>{title}</h2>
        {meta && <span className="mono">{meta}</span>}
      </div>
      <div className="panel-b">
        <div className="tw">{children}</div>
      </div>
    </section>
  )
}

function Empty({ msg }) {
  return <div className="state">{msg}</div>
}

function AddBook({ authors, categories, onAdded, onError }) {
  const [f, setF] = useState({ title: '', isbn: '', authorId: '', categoryId: '', totalCopies: 1, availableCopies: 1 })
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })
  const ready = f.title.trim() && /^[0-9]{13}$/.test(f.isbn) && f.authorId && f.categoryId

  async function submit(e) {
    e.preventDefault()
    if (!ready) return
    setBusy(true)
    try {
      await api.createBook({
        title: f.title.trim(),
        isbn: f.isbn,
        authorId: Number(f.authorId),
        categoryId: Number(f.categoryId),
        totalCopies: Number(f.totalCopies),
        availableCopies: Number(f.availableCopies),
      })
      setF({ title: '', isbn: '', authorId: '', categoryId: '', totalCopies: 1, availableCopies: 1 })
      onAdded()
    } catch (err) {
      onError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="panel catalog-card">
      <div className="panel-h" style={{ padding: 0, borderBottom: 'none' }}>
        <h2>Kitap Ekle</h2>
        <span className="mono">KATALOG KARTI</span>
      </div>
      {(!authors.length || !categories.length) && (
        <div className="note" style={{ marginTop: 12 }}>
          <span>Önce en az bir yazar ve kategori gerekli.</span>
        </div>
      )}
      <form onSubmit={submit}>
        <div className="field">
          <label className="field-label" htmlFor="nb-title">
            Başlık
          </label>
          <input id="nb-title" value={f.title} onChange={set('title')} placeholder="ör. Tutunamayanlar" />
        </div>
        <div className="field">
          <label className="field-label" htmlFor="nb-isbn">
            ISBN · 13 rakam
          </label>
          <input id="nb-isbn" value={f.isbn} onChange={set('isbn')} inputMode="numeric" placeholder="9789750000000" />
        </div>
        <div className="field">
          <label className="field-label" htmlFor="nb-author">
            Yazar
          </label>
          <select id="nb-author" value={f.authorId} onChange={set('authorId')}>
            <option value="">— seç —</option>
            {authors.map((a) => (
              <option key={a.id} value={a.id}>
                {a.name} {a.lastname}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label className="field-label" htmlFor="nb-cat">
            Kategori
          </label>
          <select id="nb-cat" value={f.categoryId} onChange={set('categoryId')}>
            <option value="">— seç —</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
        <div className="split">
          <div className="field">
            <label className="field-label" htmlFor="nb-total">
              Toplam
            </label>
            <input id="nb-total" type="number" min="1" value={f.totalCopies} onChange={set('totalCopies')} />
          </div>
          <div className="field">
            <label className="field-label" htmlFor="nb-free">
              Müsait
            </label>
            <input id="nb-free" type="number" min="0" value={f.availableCopies} onChange={set('availableCopies')} />
          </div>
        </div>
        <button className="btn primary block" type="submit" disabled={busy || !ready}>
          {busy ? (
            <>
              <span className="spinner" /> Ekleniyor…
            </>
          ) : (
            'Kataloğa ekle'
          )}
        </button>
      </form>
    </section>
  )
}
