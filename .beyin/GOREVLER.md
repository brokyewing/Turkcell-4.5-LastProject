# Görevler — Kitaplık (Turkcell bitirme)

Durum: `[ ]` açık · `[~]` devam ediyor · `[x]` bitti · `[!]` engellendi

## Açık
- [ ] **A4 — Vitrin** (G6, G7, G8, G9): JWT'ye rol claim + gateway yol-rol kontrolü; servis port publish'lerini kaldır; secret'ları env'e; Flyway bağla; README hizala.
- [ ] **A3-artık** (düşük, reviewer notu): commit-anı rollback telafisi (`afterCompletion`) için entegrasyon testi; `returnLoan` telafisini işlem kimliğine bağlama.
- [ ] **Tarayıcı E2E**: giriş → katalog → ödünç → iade → ceza, gerçek tarayıcıda bir kez elle.
- [ ] **`FineSchemaInitializer` kalıcılaştır**: şu an açılışta mükerrer `borrowid` satırlarını siler + kısmi unique index kurar (geçici). Kalıcı çözüm Flyway.

## Ajan panosu

| Kart | Rol | Durum | Kabul | Kanıt / not |
|---|---|---|---|---|
| A1 | builder | bitti | login 200+JWT; tokenlı books 200; kayıt 400/200 | 8 modül pkg rc=0; book 6/6; kayıt → "Genel" bölümü |
| A2 | builder + frontend | bitti | iki app build rc=0 | `8fd3e64`; base64url fix; 401'de oturum kapanışı |
| A3 | builder | bitti | 4 servis testi yeşil | book 6 · loan 15 · fine 6 · reservation 7 |
| A4 | builder + security-auditor | sırada | tokensuz `/api/students` 401; öğrenci token'ıyla `DELETE /api/books/1` 403 | — |

## Bitti
- [x] **İnceleme** (2026-11-08): 8 modül build, gerçek çalıştırma, 13 bulgu `PROJE.md`'de. Kod değişmedi.
- [x] **A1 — Nefes aldır** (2026-11-09): config-server native profil + Cloud 2025.0.3 hizası; `@EnableJpaAuditing` çakışması; seed (S1001/pass123, yazar/kategori/kitap/kopya, bölümler); gateway'de public kayıt; `StudentCreateRequest` + `@Valid`; kayıtta varsayılan "Genel" bölümü.
- [x] **A2 — Frontend'i bağla** (2026-11-09): `frontend-taste` gerçek API'ye bağlandı (Page→dizi, kopya seçimi, ödünç/iade, ödenmemiş ceza); `library-frontend` login + Page düzeltmesi; proxy 18090.
- [x] **A3 — İş kuralları** (2026-11-09): atomik stok düşme/artma (JPQL `availableCopies > 0`, increment'te `totalCopies` tavanı), loan-service Feign + rollback telafisi, iadede stok geri, rezervasyon `@Scheduled` expire+fulfill (turlar arası oversell koruması), `fines.borrowid` kısmi unique index + `DataIntegrityViolationException`, Kafka `afterCommit`.
- [x] **Merge** (2026-11-09): `a1-nefes` + `a2-frontend` + `a3-kurallar` → `main` (`2841116`), çakışmasız; merge sonrası 8 modül testi/paketi yeşil.
