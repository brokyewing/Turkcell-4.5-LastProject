// JWT payload çözücü. İmza doğrulaması SUNUCUDA; burada sadece studentId/name/role/sub okunur.
// atob ikili (Latin-1) string döndürür; Türkçe karakterler için UTF-8 çöz.
export function decodeToken(token) {
  if (!token) return null
  try {
    // JWT base64url kodludur: '-' → '+', '_' → '/' ve '=' dolgusu gerekir.
    const b64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const pad = b64.padEnd(b64.length + (4 - b64.length % 4) % 4, '=')
    const bin = atob(pad)
    const bytes = Uint8Array.from(bin, (c) => c.charCodeAt(0))
    const payload = JSON.parse(new TextDecoder().decode(bytes))
    // role claim'i gateway ile aynı semantik: yoksa STUDENT varsay.
    const role = typeof payload.role === 'string' && payload.role ? payload.role : 'STUDENT'
    return { studentId: payload.studentId, name: payload.name, sub: payload.sub, role }
  } catch {
    return null
  }
}
