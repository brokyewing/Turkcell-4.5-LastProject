import { useMemo, useState } from 'react'
import { api } from './api'
import { useData } from './data'
import { Kpi, Panel, Field, Loading, ErrorState, EmptyState, useToast } from './ui'
import { Icon } from './icons'

/* ============================================================
   GENEL BAKIŞ — KPI'lar + son eklenen kitaplar
   ============================================================ */
export function Dashboard() {
  const { books, authors, categories, loading, error, reload } = useData()

  const availableCopies = useMemo(
    () => books.reduce((sum, b) => sum + (b.availableCopies || 0), 0),
    [books],
  )

  if (error) return <ErrorState message={error} onRetry={reload} />

  const recent = [...books].slice(-6).reverse()

  return (
    <>
      <div className="kpi-grid">
        <Kpi label="Kitap" value={books.length} icon={Icon.Book} tone="blue" loading={loading} />
        <Kpi label="Yazar" value={authors.length} icon={Icon.Users} tone="blue" loading={loading} />
        <Kpi label="Kategori" value={categories.length} icon={Icon.Tag} tone="blue" loading={loading} />
        <Kpi label="Mevcut Kopya" value={availableCopies} icon={Icon.Copies} tone="green" loading={loading} />
      </div>

      <Panel title="Son Eklenen Kitaplar">
        {loading ? <Loading /> : recent.length === 0 ? (
          <EmptyState message="Henüz kitap yok. 'Kitaplar' sekmesinden ilk kitabı ekleyebilirsin." />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>#</th><th>Başlık</th><th>Yazar</th><th>Kategori</th><th>Durum</th></tr>
              </thead>
              <tbody>
                {recent.map((b) => (
                  <tr key={b.id}>
                    <td className="num">{b.id}</td>
                    <td><span className="title">{b.title}</span></td>
                    <td>{b.authorName} {b.authorLastname}</td>
                    <td>{b.categoryName}</td>
                    <td><CopyBadge available={b.availableCopies} total={b.totalCopies} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>
    </>
  )
}

function CopyBadge({ available, total }) {
  const ok = available > 0
  return (
    <span className={`badge ${ok ? 'ok' : 'warn'}`}>
      {ok ? <Icon.Check size={13} /> : <Icon.Alert size={13} />}
      {available}/{total}
    </span>
  )
}

/* Arama + Ekle butonlu ortak araç çubuğu */
function Toolbar({ query, setQuery, placeholder, onAdd, adding }) {
  return (
    <>
      <div className="search">
        <Icon.Search />
        <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder={placeholder} aria-label="Ara" />
      </div>
      <button className="btn primary" onClick={onAdd} aria-expanded={adding}>
        <Icon.Plus size={18} /> {adding ? 'Kapat' : 'Ekle'}
      </button>
    </>
  )
}

/* ============================================================
   KİTAPLAR
   ============================================================ */
export function BooksView() {
  const { books, authors, categories, loading, error, reload } = useData()
  const toast = useToast()
  const [query, setQuery] = useState('')
  const [adding, setAdding] = useState(false)

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return books
    return books.filter((b) =>
      b.title.toLowerCase().includes(q) ||
      b.isbn.includes(q) ||
      `${b.authorName} ${b.authorLastname}`.toLowerCase().includes(q),
    )
  }, [books, query])

  return (
    <Panel
      title={`Kitaplar (${books.length})`}
      actions={<Toolbar query={query} setQuery={setQuery} placeholder="Başlık, ISBN veya yazar ara…" onAdd={() => setAdding((v) => !v)} adding={adding} />}
    >
      {adding && (
        <div className="collapse">
          <div className="inner">
            <BookForm
              authors={authors}
              categories={categories}
              onDone={async () => { setAdding(false); await reload(); toast('Kitap eklendi.') }}
              onError={(m) => toast(m, 'err')}
            />
          </div>
        </div>
      )}

      {error ? <ErrorState message={error} onRetry={reload} />
        : loading ? <Loading />
        : filtered.length === 0 ? <EmptyState message={query ? 'Aramanla eşleşen kitap yok.' : 'Henüz kitap yok.'} />
        : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>#</th><th>Başlık</th><th>ISBN</th><th>Yazar</th><th>Kategori</th><th>Durum</th></tr>
              </thead>
              <tbody>
                {filtered.map((b) => (
                  <tr key={b.id}>
                    <td className="num">{b.id}</td>
                    <td><span className="title">{b.title}</span></td>
                    <td className="num">{b.isbn}</td>
                    <td>{b.authorName} {b.authorLastname}</td>
                    <td>{b.categoryName}</td>
                    <td><CopyBadge available={b.availableCopies} total={b.totalCopies} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
    </Panel>
  )
}

function BookForm({ authors, categories, onDone, onError }) {
  const [form, setForm] = useState({ title: '', isbn: '', authorId: '', categoryId: '', totalCopies: 1, availableCopies: 1 })
  const [submitting, setSubmitting] = useState(false)
  const [touched, setTouched] = useState(false)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const isbnValid = /^[0-9]{13}$/.test(form.isbn)
  const canSubmit = form.title && isbnValid && form.authorId && form.categoryId && authors.length && categories.length

  async function submit(e) {
    e.preventDefault()
    setTouched(true)
    if (!canSubmit) return
    setSubmitting(true)
    try {
      await api.createBook({
        title: form.title,
        isbn: form.isbn,
        authorId: Number(form.authorId),
        categoryId: Number(form.categoryId),
        totalCopies: Number(form.totalCopies),
        availableCopies: Number(form.availableCopies),
      })
      onDone()
    } catch (e) {
      onError(e.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (!authors.length || !categories.length) {
    return <EmptyState message="Önce en az bir yazar ve bir kategori eklemelisin." />
  }

  return (
    <form onSubmit={submit}>
      <div className="form-grid">
        <Field label="Başlık" required>
          <input value={form.title} onChange={set('title')} placeholder="ör. 1984" required />
        </Field>
        <Field label="ISBN" required hint="Tam 13 rakam" error={touched && !isbnValid ? 'ISBN 13 rakam olmalı' : null}>
          <input className={touched && !isbnValid ? 'invalid' : ''} value={form.isbn} onChange={set('isbn')} inputMode="numeric" placeholder="9780451524935" />
        </Field>
        <Field label="Yazar" required>
          <select value={form.authorId} onChange={set('authorId')} required>
            <option value="">— seç —</option>
            {authors.map((a) => <option key={a.id} value={a.id}>{a.name} {a.lastname}</option>)}
          </select>
        </Field>
        <Field label="Kategori" required>
          <select value={form.categoryId} onChange={set('categoryId')} required>
            <option value="">— seç —</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </Field>
        <Field label="Toplam kopya" required>
          <input type="number" min="1" value={form.totalCopies} onChange={set('totalCopies')} />
        </Field>
        <Field label="Mevcut kopya" required>
          <input type="number" min="0" value={form.availableCopies} onChange={set('availableCopies')} />
        </Field>
      </div>
      <div className="form-actions">
        <button type="submit" className="btn primary" disabled={submitting || !canSubmit}>
          {submitting ? <><span className="spinner" /> Kaydediliyor…</> : <><Icon.Check /> Kaydet</>}
        </button>
      </div>
    </form>
  )
}

/* ============================================================
   YAZARLAR & KATEGORİLER — aynı desen
   ============================================================ */
export function AuthorsView() {
  const { authors, loading, error, reload } = useData()
  const toast = useToast()
  const [query, setQuery] = useState('')
  const [adding, setAdding] = useState(false)
  const [form, setForm] = useState({ name: '', lastname: '' })
  const [submitting, setSubmitting] = useState(false)

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return q ? authors.filter((a) => `${a.name} ${a.lastname}`.toLowerCase().includes(q)) : authors
  }, [authors, query])

  async function submit(e) {
    e.preventDefault()
    setSubmitting(true)
    try {
      await api.createAuthor(form)
      setForm({ name: '', lastname: '' }); setAdding(false); await reload(); toast('Yazar eklendi.')
    } catch (e) { toast(e.message, 'err') } finally { setSubmitting(false) }
  }

  return (
    <Panel
      title={`Yazarlar (${authors.length})`}
      actions={<Toolbar query={query} setQuery={setQuery} placeholder="Yazar ara…" onAdd={() => setAdding((v) => !v)} adding={adding} />}
    >
      {adding && (
        <div className="collapse"><div className="inner">
          <form onSubmit={submit}>
            <div className="form-grid">
              <Field label="Ad" required><input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></Field>
              <Field label="Soyad" required><input value={form.lastname} onChange={(e) => setForm({ ...form, lastname: e.target.value })} required /></Field>
            </div>
            <div className="form-actions">
              <button type="submit" className="btn primary" disabled={submitting}>{submitting ? <><span className="spinner" /> Kaydediliyor…</> : <><Icon.Check /> Kaydet</>}</button>
            </div>
          </form>
        </div></div>
      )}
      {error ? <ErrorState message={error} onRetry={reload} />
        : loading ? <Loading />
        : filtered.length === 0 ? <EmptyState message={query ? 'Eşleşen yazar yok.' : 'Henüz yazar yok.'} />
        : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>#</th><th>Ad</th><th>Soyad</th></tr></thead>
              <tbody>{filtered.map((a) => <tr key={a.id}><td className="num">{a.id}</td><td><span className="title">{a.name}</span></td><td>{a.lastname}</td></tr>)}</tbody>
            </table>
          </div>
        )}
    </Panel>
  )
}

export function CategoriesView() {
  const { categories, loading, error, reload } = useData()
  const toast = useToast()
  const [query, setQuery] = useState('')
  const [adding, setAdding] = useState(false)
  const [name, setName] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return q ? categories.filter((c) => c.name.toLowerCase().includes(q)) : categories
  }, [categories, query])

  async function submit(e) {
    e.preventDefault()
    setSubmitting(true)
    try {
      await api.createCategory({ name })
      setName(''); setAdding(false); await reload(); toast('Kategori eklendi.')
    } catch (e) { toast(e.message, 'err') } finally { setSubmitting(false) }
  }

  return (
    <Panel
      title={`Kategoriler (${categories.length})`}
      actions={<Toolbar query={query} setQuery={setQuery} placeholder="Kategori ara…" onAdd={() => setAdding((v) => !v)} adding={adding} />}
    >
      {adding && (
        <div className="collapse"><div className="inner">
          <form onSubmit={submit}>
            <div className="form-grid">
              <Field label="Ad" required><input value={name} onChange={(e) => setName(e.target.value)} required /></Field>
            </div>
            <div className="form-actions">
              <button type="submit" className="btn primary" disabled={submitting}>{submitting ? <><span className="spinner" /> Kaydediliyor…</> : <><Icon.Check /> Kaydet</>}</button>
            </div>
          </form>
        </div></div>
      )}
      {error ? <ErrorState message={error} onRetry={reload} />
        : loading ? <Loading />
        : filtered.length === 0 ? <EmptyState message={query ? 'Eşleşen kategori yok.' : 'Henüz kategori yok.'} />
        : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>#</th><th>Ad</th></tr></thead>
              <tbody>{filtered.map((c) => <tr key={c.id}><td className="num">{c.id}</td><td><span className="title">{c.name}</span></td></tr>)}</tbody>
            </table>
          </div>
        )}
    </Panel>
  )
}
