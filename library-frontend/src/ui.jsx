// Paylaşılan küçük UI bileşenleri — tek yerden, tutarlı.
import { createContext, useContext, useState, useCallback } from 'react'
import { Icon } from './icons'

/* ---------- KPI kartı ---------- */
export function Kpi({ label, value, icon: IconCmp, tone = 'blue', loading }) {
  return (
    <div className="kpi">
      <div>
        <div className="label">{label}</div>
        <div className="value">{loading ? '—' : value}</div>
      </div>
      <div className={`ico ${tone}`}><IconCmp size={20} /></div>
    </div>
  )
}

/* ---------- Panel ---------- */
export function Panel({ title, actions, children }) {
  return (
    <section className="panel">
      {(title || actions) && (
        <div className="panel-head">
          <h2>{title}</h2>
          <div className="toolbar">{actions}</div>
        </div>
      )}
      {children}
    </section>
  )
}

/* ---------- Form alanı ---------- */
export function Field({ label, required, hint, error, children }) {
  return (
    <div className="field">
      <label>{label}{required && <span className="req"> *</span>}</label>
      {children}
      {error ? <span className="err">{error}</span> : hint ? <span className="hint">{hint}</span> : null}
    </div>
  )
}

/* ---------- Durum göstergeleri ---------- */
export function Loading({ label = 'Yükleniyor…' }) {
  return <div className="state"><span className="spinner" /> {label}</div>
}
export function ErrorState({ message, onRetry }) {
  return (
    <div className="state error">
      <Icon.Alert /> {message}
      {onRetry && <div style={{ marginTop: 12 }}><button className="btn" onClick={onRetry}><Icon.Refresh /> Tekrar dene</button></div>}
    </div>
  )
}
export function EmptyState({ message }) {
  return <div className="state">{message}</div>
}

/* ---------- Toast (aria-live ile erişilebilir) ---------- */
const ToastCtx = createContext(null)
export const useToast = () => useContext(ToastCtx)

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const push = useCallback((message, type = 'ok') => {
    // Date.now yerine artan sayaç kullanmıyoruz; basit benzersiz anahtar yeter
    const id = Math.random().toString(36).slice(2)
    setToasts((t) => [...t, { id, message, type }])
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 4000)
  }, [])
  return (
    <ToastCtx.Provider value={push}>
      {children}
      <div className="toasts" aria-live="polite" aria-atomic="false">
        {toasts.map((t) => (
          <div key={t.id} className={`toast ${t.type}`} role="status">
            {t.type === 'ok' ? <Icon.Check /> : <Icon.Alert />}
            <span>{t.message}</span>
          </div>
        ))}
      </div>
    </ToastCtx.Provider>
  )
}
