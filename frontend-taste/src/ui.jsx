import { useEffect, useRef } from 'react'

/* Ortak görsel parçalar: marka işareti, tipografik kapak, etiket, modal, tarih. */

export function cx(...parts) {
  return parts.filter(Boolean).join(' ')
}

/* Kitap sırtı markası — 4 dikey çubuk, sonuncusu mühür renginde. */
export function Mark() {
  return (
    <span className="mark" aria-hidden="true">
      <i />
      <i />
      <i />
      <i />
    </span>
  )
}

/* Kategoriye göre deterministik cilt paleti. */
const PALETTE_BY_CATEGORY = {
  roman: 'brick',
  'bilim kurgu': 'pine',
  öykü: 'ochre',
  şiir: 'plum',
  tarih: 'slate',
  felsefe: 'ink',
  bilim: 'pine',
  sanat: 'plum',
}

const FALLBACK = ['brick', 'pine', 'ochre', 'plum', 'slate', 'ink']

function paletteFor(category) {
  const key = String(category || '').trim().toLocaleLowerCase('tr')
  if (PALETTE_BY_CATEGORY[key]) return PALETTE_BY_CATEGORY[key]
  let h = 0
  for (let i = 0; i < key.length; i += 1) h = (h * 31 + key.charCodeAt(i)) % 997
  return FALLBACK[h % FALLBACK.length]
}

/* Kitap kapağı: cilt dokusu + sırt yazısı + editöryel tipografi. */
export function Cover({ book, className }) {
  const author = [book.authorName, book.authorLastname].filter(Boolean).join(' ')
  const year = book.publicationDate ? String(book.publicationDate).slice(0, 4) : ''
  return (
    <div className={cx('cover', `cv-${paletteFor(book.categoryName)}`, className)}>
      <span className="cv-spine">
        <span>{author || 'Kitaplık'}</span>
      </span>
      <div className="cv-face">
        <span className="cv-cat">{book.categoryName || 'Katalog'}</span>
        <span className="cv-title">{book.title}</span>
        <span className="rule" />
        <span className="cv-foot">
          <span>{author || '—'}</span>
          <span>{year}</span>
        </span>
      </div>
    </div>
  )
}

const TONES = ['ok', 'warn', 'dan', 'ink']
export function Tag({ tone = 'ink', children }) {
  const t = TONES.includes(tone) ? tone : 'ink'
  return <span className={`tag ${t}`}>{children}</span>
}

/* Erişilebilir modal: Esc ile kapanır, arka plana tıklanınca kapanır, odağı içeride tutar. */
export function Modal({ open, onClose, labelledBy, children }) {
  const sheetRef = useRef(null)
  const lastFocused = useRef(null)

  useEffect(() => {
    if (!open) return undefined
    lastFocused.current = document.activeElement
    const node = sheetRef.current
    const focusables = () =>
      Array.from(
        node?.querySelectorAll(
          'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
        ) || [],
      )
    focusables()[0]?.focus()

    function onKey(e) {
      if (e.key === 'Escape') {
        e.preventDefault()
        onClose()
        return
      }
      if (e.key !== 'Tab') return
      const list = focusables()
      if (!list.length) return
      const first = list[0]
      const last = list[list.length - 1]
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault()
        last.focus()
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault()
        first.focus()
      }
    }

    document.addEventListener('keydown', onKey)
    const prevOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = prevOverflow
      if (lastFocused.current instanceof HTMLElement) lastFocused.current.focus()
    }
  }, [open, onClose])

  if (!open) return null
  return (
    <div className="backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="sheet" role="dialog" aria-modal="true" aria-labelledby={labelledBy} ref={sheetRef}>
        {children}
      </div>
    </div>
  )
}

/* ---------- tarih / sayı yardımcıları ---------- */

export function money(n) {
  const v = Number(n) || 0
  return v.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function fmtDate(d) {
  if (!d) return '—'
  const x = new Date(d)
  if (Number.isNaN(x.getTime())) return String(d)
  return x.toLocaleDateString('tr-TR', { day: '2-digit', month: 'short', year: 'numeric' })
}

export function fmtDateLong(d) {
  if (!d) return '—'
  const x = new Date(d)
  if (Number.isNaN(x.getTime())) return String(d)
  return x.toLocaleDateString('tr-TR', { day: 'numeric', month: 'long', year: 'numeric' })
}

export function todayLong() {
  return new Date().toLocaleDateString('tr-TR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  })
}

export function startOfDay(d) {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  return x
}

/* Bugüne göre kalan gün (negatif = gecikme). Backend ChronoUnit.DAYS ile hizalı. */
export function daysLeft(dueDate) {
  if (!dueDate) return null
  const diff = startOfDay(dueDate).getTime() - startOfDay(new Date()).getTime()
  return Math.round(diff / 86400000)
}

export function isoDate(d) {
  const x = new Date(d)
  const p = (n) => String(n).padStart(2, '0')
  return `${x.getFullYear()}-${p(x.getMonth() + 1)}-${p(x.getDate())}`
}

export function addDays(d, n) {
  const x = new Date(d)
  x.setDate(x.getDate() + n)
  return x
}
