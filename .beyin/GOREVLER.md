# Görevler — Kitaplık (Turkcell bitirme)

Durum: `[ ]` açık · `[~]` devam ediyor · `[x]` bitti · `[!]` engellendi

## Açık
- [ ] **IDOR — sahiplik kontrolü** (YÜKSEK, `reviewer`+`security-auditor` bulgusu): gateway token'dan gelen
  `X-Student-Id`'yi downstream'e taşıyor ama **hiçbir servis okumuyor**. Bugün STUDENT token'ıyla
  `GET /api/students/{herhangiId}` (PII), `GET /api/loans/members/{herhangiId}`,
  `POST /api/loans {studentId: başkası}` çalışıyor. Kabul: STUDENT kendi id'si dışındaki kaynaklara 403.
- [ ] **CORS** (ORTA): gateway'de CORS yok → tarayıcıdan ayrı origin'deki frontend API'yi çağıramaz
  (preflight `OPTIONS` 403). Karar: frontend aynı origin'e ters-proxy mi, gateway'e `globalcors` mu?
- [ ] **Frontend `role` kullanmıyor** (ORTA): `LoginResponse.role` ölü; `frontend-taste/App.jsx` herkese
  "Kütüphaneci" sekmesini gösteriyor → STUDENT tıklayınca 403 yağmuru.
- [ ] **Tarayıcı E2E**: giriş → katalog → ödünç → iade → ceza, gerçek tarayıcıda bir kez elle.
- [ ] **A3-artık** (düşük): commit-anı rollback telafisi (`afterCompletion`) için entegrasyon testi;
  `returnLoan` telafisini işlem kimliğine bağlama.
- [ ] **`configurations/` ölü mü?** (kanıtlandı: hiçbir serviste `spring-cloud-starter-config` yok,
  hiçbir `application.yml`'de `spring.config.import` yok) — silme kararı **kullanıcıda**.
- [ ] **`03-microservices/4.5-microservices`** git deposu 0 commit'li — karar kullanıcıda.

## Ajan panosu

| Kart | Rol | Durum | Kabul | Kanıt / not |
|---|---|---|---|---|
| A1 | builder | bitti | login 200+JWT; tokenlı books 200 | book 6/6; kayıt → "Genel" |
| A2 | builder + frontend | bitti | iki app build rc=0 | `8fd3e64`; 401'de oturum kapanışı |
| A3 | builder | bitti | 4 servis testi yeşil | book 6 · loan 15 · fine 6 · reservation 7 |
| A4a | builder | bitti | STUDENT `DELETE /api/books/1` 403 | `8cbd5f5`; gateway 8 test |
| A4b | devops | bitti | yalnız gateway host portu | `d06ea82`; port tablosu doğrulandı |
| A4c | builder | bitti | `ddl-auto: none` ile açılış | `a48a942`; canlı DB "up to date" |
| A4d | builder | bitti | iki Flyway kırıcısı + seed/env | `f56bf2b`; canlı+taze DB kanıtı |
| İnceleme | reviewer + security-auditor | bitti | şiddet sıralı bulgu | 2 KIRICI bulundu → A4d'de kapandı |

## Bitti
- [x] **İnceleme** (2026-11-08): 8 modül build, gerçek çalıştırma, 13 bulgu `PROJE.md`'de.
- [x] **A1 — Nefes aldır** (2026-11-09): config-server native profil + Cloud 2025.0.3 hizası; `@EnableJpaAuditing` çakışması; seed; gateway'de public kayıt; `StudentCreateRequest` + `@Valid`; kayıtta varsayılan "Genel" bölümü.
- [x] **A2 — Frontend'i bağla** (2026-11-09): `frontend-taste` gerçek API'ye bağlandı; `library-frontend` login + Page düzeltmesi; proxy 18090.
- [x] **A3 — İş kuralları** (2026-11-09): atomik stok düşme/artma, loan Feign + rollback telafisi, rezervasyon `@Scheduled` expire+fulfill, `fines.borrowid` unique index, Kafka `afterCommit`.
- [x] **A1+A2+A3 merge** (2026-11-09): `main` `2841116`, çakışmasız; 8 modül yeşil.
- [x] **A4 — Vitrin/güvenlik** (2026-11-09): rol modeli (`STUDENT`/`LIBRARIAN`) + JWT `role` claim + kütüphaneci seed; gateway yol-rol allow-list, JSON 401/403, `X-Student-Id`/`X-Role` spoofing temizliği; compose'da yalnız gateway host portu, altyapı `127.0.0.1`, secret'lar `.env`'den, gerçek actuator healthcheck; 5 servise Flyway `V1__baseline.sql` (canlı `pg_dump`), servis başına history tablosu, `ddl-auto: none`.
- [x] **A4d — A4 kırıcı düzeltmeleri** (2026-11-09): `V2__add_student_role.sql` (A4a entity ↔ A4c baseline çakışması), V1'ler idempotent + `baseline-version: 0` (temiz kurulumda servis sırası bağımsız), kütüphaneci parolası `DEMO_LIBRARIAN_PASSWORD`'e taşındı, `discovery.locator` kapalı, `/actuator` → `/actuator/health`, `FineSchemaInitializer` silindi.
- [x] **A4 merge + E2E** (2026-11-09): `main` `9c69a49`; 11 konteyner sağlıklı; yetkilendirme matrisi ve ceza zinciri canlı doğrulandı.
- [x] **`.beyin/` commit**, **`target/` temizliği** (482.9 MB), **`Turkcell-Java-Odev` WIP** (`cef1fa7` + `d597029`), **klasör gruplandırma** (2026-11-09).
