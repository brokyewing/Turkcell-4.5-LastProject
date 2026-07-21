import { useEffect, useState } from 'react'
import { api } from './api'

// KÜTÜPHANECİ yüzeyi: sistemin nabzı. Dashboard KPI + tablolar + kitap ekle.
export default function AdminView() {
  const [data, setData] = useState({ books: [], students: [], loans: [], fines: [], authors: [], categories: [] })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [toast, setToast] = useState(null)

  async function load() {
    setLoading(true); setError(null)
    try {
      const [books, students, loans, fines, authors, categories] = await Promise.all([
        api.getBooks(), api.getStudents().catch(() => []), api.getLoans().catch(() => []),
        api.getFines().catch(() => []), api.getAuthors(), api.getCategories(),
      ])
      setData({ books, students: students || [], loans: loans || [], fines: fines || [], authors, categories })
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => { load() }, [])

  const openLoans = data.loans.filter((l) => !l.isReturned).length
  const unpaidFines = data.fines.filter((f) => !f.isPaid)
  const unpaidTotal = unpaidFines.reduce((s, f) => s + (f.amount || 0), 0)

  function notify(m) { setToast(m); setTimeout(() => setToast(null), 3000) }

  if (error) return <div className="page"><div className="state err">Hata: {error}</div></div>

  return (
    <div className="page">
      <div className="sec-title rise">Genel Bakış</div>

      <div className="kpis rise">
        <Kpi label="Kitap" value={data.books.length} loading={loading} />
        <Kpi label="Üye" value={data.students.length} loading={loading} />
        <Kpi label="Açık Ödünç" value={openLoans} loading={loading} />
        <Kpi label="Ödenmemiş Ceza" value={`${unpaidTotal}₺`} warn loading={loading} />
      </div>

      <div className="grid2">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          <Panel title={`Son Ödünçler (${data.loans.length})`}>
            {data.loans.length === 0 ? <Empty msg="Henüz ödünç yok." /> : (
              <table>
                <thead><tr><th>#</th><th>Öğrenci</th><th>Kopya</th><th>Durum</th></tr></thead>
                <tbody>
                  {[...data.loans].reverse().slice(0, 6).map((l) => (
                    <tr key={l.id}>
                      <td className="num">{l.id}</td>
                      <td className="num">{l.studentId}</td>
                      <td className="num">{l.copyBookId}</td>
                      <td>{l.isReturned
                        ? <span className="badge ok">İade edildi</span>
                        : <span className="badge warn">Ödünçte</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Panel>

          <Panel title={`Cezalar (${data.fines.length})`}>
            {data.fines.length === 0 ? <Empty msg="Ceza yok — herkes zamanında iade ediyor." /> : (
              <table>
                <thead><tr><th>#</th><th>Öğrenci</th><th>Tutar</th><th>Tür</th><th>Durum</th></tr></thead>
                <tbody>
                  {data.fines.map((f) => (
                    <tr key={f.id}>
                      <td className="num">{f.id}</td>
                      <td className="num">{f.studentId}</td>
                      <td className="num">{f.amount}₺</td>
                      <td><span className="badge warn">{f.fineType}</span></td>
                      <td>{f.isPaid
                        ? <span className="badge ok">Ödendi</span>
                        : <span className="badge dan">Bekliyor</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Panel>
        </div>

        <AddBook authors={data.authors} categories={data.categories} onAdded={() => { load(); notify('Kitap eklendi.') }} onError={notify} />
      </div>

      {toast && <div className="toasts"><div className="toast">{toast}</div></div>}
    </div>
  )
}

function Kpi({ label, value, warn, loading }) {
  return (
    <div className={`kpi ${warn ? 'warn' : ''}`}>
      <div className="l">{label}</div>
      <div className="v">{loading ? '—' : value}</div>
    </div>
  )
}
function Panel({ title, children }) {
  return (
    <div className="panel">
      <div className="panel-h"><h2>{title}</h2></div>
      <div className="panel-b"><div className="tw">{children}</div></div>
    </div>
  )
}
function Empty({ msg }) { return <div className="state">{msg}</div> }

function AddBook({ authors, categories, onAdded, onError }) {
  const [f, setF] = useState({ title: '', isbn: '', authorId: '', categoryId: '', totalCopies: 1, availableCopies: 1 })
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })
  const ready = f.title && /^[0-9]{13}$/.test(f.isbn) && f.authorId && f.categoryId

  async function submit(e) {
    e.preventDefault()
    if (!ready) return
    setBusy(true)
    try {
      await api.createBook({
        title: f.title, isbn: f.isbn,
        authorId: Number(f.authorId), categoryId: Number(f.categoryId),
        totalCopies: Number(f.totalCopies), availableCopies: Number(f.availableCopies),
      })
      setF({ title: '', isbn: '', authorId: '', categoryId: '', totalCopies: 1, availableCopies: 1 })
      onAdded()
    } catch (e) { onError(e.message) } finally { setBusy(false) }
  }

  return (
    <div className="form-card">
      <h2>Kitap Ekle</h2>
      {(!authors.length || !categories.length) && <div className="muted" style={{ fontSize: 13, marginBottom: 10 }}>Önce yazar ve kategori gerekli.</div>}
      <form onSubmit={submit}>
        <div><label>Başlık</label><input value={f.title} onChange={set('title')} placeholder="ör. Sefiller" /></div>
        <div><label>ISBN (13 rakam)</label><input value={f.isbn} onChange={set('isbn')} inputMode="numeric" placeholder="9789750000000" /></div>
        <div><label>Yazar</label>
          <select value={f.authorId} onChange={set('authorId')}>
            <option value="">— seç —</option>
            {authors.map((a) => <option key={a.id} value={a.id}>{a.name} {a.lastname}</option>)}
          </select>
        </div>
        <div><label>Kategori</label>
          <select value={f.categoryId} onChange={set('categoryId')}>
            <option value="">— seç —</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
        <div style={{ display: 'flex', gap: 12 }}>
          <div style={{ flex: 1 }}><label>Toplam</label><input type="number" min="1" value={f.totalCopies} onChange={set('totalCopies')} /></div>
          <div style={{ flex: 1 }}><label>Müsait</label><input type="number" min="0" value={f.availableCopies} onChange={set('availableCopies')} /></div>
        </div>
        <button className="btn primary" type="submit" disabled={busy || !ready} style={{ justifyContent: 'center' }}>
          {busy ? <><span className="spinner" /> Ekleniyor…</> : 'Ekle'}
        </button>
      </form>
    </div>
  )
}
