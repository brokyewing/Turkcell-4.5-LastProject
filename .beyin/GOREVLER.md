# Görevler — Kitaplık (Turkcell bitirme)

Durum: `[ ]` açık · `[~]` devam ediyor · `[x]` bitti · `[!]` engellendi

## Açık
- [ ] **CORS** (ORTA): gateway'de CORS yok → tarayıcıdan ayrı origin'deki frontend API'yi çağıramaz
  (preflight `OPTIONS` 403). Karar: frontend aynı origin'e ters-proxy mi, gateway'e `globalcors` mu?
- [ ] **Frontend `role` kullanmıyor** (ORTA): `LoginResponse.role` geliyor ama `frontend-taste/App.jsx`
  herkese "Kütüphaneci" sekmesini gösteriyor → STUDENT tıklayınca 403 yağmuru.
- [ ] **Tarayıcı E2E**: giriş → katalog → ödünç → iade → ceza, gerçek tarayıcıda bir kez elle.
- [ ] **`main` push kararı**: `main` = `3ac67bd`, `origin/main` = `ce3d349` (7 commit ileride). Kullanıcı onayı bekliyor.
- [ ] **Sertleştirme turu** (düşük, engelleyici değil): `IdentityGuard` fail-open (NetworkPolicy / iç secret);
  guard'sız `MemberController` + `@RequestBody Student` mass-assignment; 403/404 varlık oracle'ı;
  `IdentityGuard` 4 kopya → ortak modül.
- [ ] **A3-artık** (düşük): commit-anı rollback telafisi (`afterCompletion`) için entegrasyon testi;
  `returnLoan` telafisini işlem kimliğine bağlama.
- [ ] **`03-microservices/4.5-microservices`** git deposu 0 commit'li — karar kullanıcıda.
- [ ] **Vault commit `fe31ff0`** (Obsidian) yerelde, push edilmedi — karar kullanıcıda.

## Ajan panosu

| Kart | Rol | Durum | Kabul | Kanıt / not |
|---|---|---|---|---|
| C1 | devops | bitti | `configurations/` yok; config-server health 200 | `bed40c7`; compose/build doğrulandı |
| C2 | builder | bitti | 5 modül test yeşil; Feign bozulmadı | `616ef77`; 22+9+9+7+18 test |
| C2-E2E | — | bitti | 19/19 sahiplik senaryosu | `e2e-c2.ps1` 0 FAIL; canlı konteynerler |
| C2-Kontrol | reviewer | bitti | şiddet sıralı bulgu | 1 KIRICI iddia → canlıda sömürülemez çıktı |
| GW-Sert | builder | bitti | gateway 27 test; bozuk yol 403 (mutasyonla kanıt) | `3038339`+`3ac67bd`; canlı 403/401 |
| GW-Kontrol | reviewer | bitti | KIRICI/YÜKSEK yok | 2 ORTA → test sıkılaştırıldı (`3ac67bd`) |

## Bitti
- [x] **C1 — ölü `configurations/` kaldırıldı** (2026-11-09): 5 yml silindi; `config-server/Dockerfile`
  COPY satırları, compose volume ve `search-locations` temizlendi (`classpath:/`). config-server
  **kalıyor** (8 servislik mimarinin parçası) ama istemciye yapılandırma sunmuyor — hiçbir serviste
  `spring-cloud-starter-config`/`spring.config.import` yok. Kanıt: imaj derlenip ayağa kalktı, health 200.
- [x] **C2 — IDOR kapatıldı** (2026-11-09): gateway'de path-id ↔ token `studentId` eşitliği
  (`/api/students/{id}`, `/api/loans/members/{id}`, `/api/borrow/me/{id}`, `/api/fines/students/{id}/total`);
  4 serviste `IdentityGuard` + `ForbiddenException` + 403 handler. LIBRARIAN serbest, `X-Role` yoksa
  iç çağrı sayılır (Feign bozulmadı), `X-Student-Id` + STUDENT → sahiplik zorunlu.
  Canlı matris 19/19: kendi kaynak 200 · başkası 403 · spoof 403 · gerçek ödünç/iade 200.
- [x] **C1+C2 merge** (2026-11-09): `main` `2c8a334`, çakışmasız; birleşik hâlde 71 test yeşil.
- [x] **Gateway sertleştirme** (2026-11-09): public yol tam eşleşme (`/api/auth/login`), public dalda
  kimlik başlığı temizliği, `../`/`//`/`;`/`\`/`%2e`/`%2f` içeren yol 403. `main` `3038339`;
  reviewer incelemesi sonrası testler gerçek 403'ü doğrulayacak şekilde sıkılaştırıldı (`3ac67bd`) —
  guard devre dışı bırakılınca tam 5 test kırılıyor (mutasyon kanıtı). Gateway 27 test.
- [x] **Test verisi temizlendi**: loans 0 · fines 0 · reservations 0 · stoklar `available = total`.
- [x] **Branş/worktree temizliği**: `c1-config`, `c2-idor` birleşti→silindi; `-wt/c1`, `-wt/c2` kaldırıldı.
- [x] **İnceleme** (2026-11-08): 8 modül build, gerçek çalıştırma, 13 bulgu `PROJE.md`'de.
- [x] **A1 — Nefes aldır** (2026-11-09): config-server native profil + Cloud 2025.0.3 hizası; `@EnableJpaAuditing` çakışması; seed; gateway'de public kayıt; `StudentCreateRequest` + `@Valid`; kayıtta varsayılan "Genel" bölümü.
- [x] **A2 — Frontend'i bağla** (2026-11-09): `frontend-taste` gerçek API'ye bağlandı; `library-frontend` login + Page düzeltmesi; proxy 18090.
- [x] **A3 — İş kuralları** (2026-11-09): atomik stok düşme/artma, loan Feign + rollback telafisi, rezervasyon `@Scheduled` expire+fulfill, `fines.borrowid` unique index, Kafka `afterCommit`.
- [x] **A1+A2+A3 merge** (2026-11-09): `main` `2841116`, çakışmasız; 8 modül yeşil.
- [x] **A4 — Vitrin/güvenlik** (2026-11-09): rol modeli (`STUDENT`/`LIBRARIAN`) + JWT `role` claim + kütüphaneci seed; gateway yol-rol allow-list, JSON 401/403, `X-Student-Id`/`X-Role` spoofing temizliği; compose'da yalnız gateway host portu, altyapı `127.0.0.1`, secret'lar `.env`'den, gerçek actuator healthcheck; 5 servise Flyway `V1__baseline.sql` (canlı `pg_dump`), servis başına history tablosu, `ddl-auto: none`.
- [x] **A4d — A4 kırıcı düzeltmeleri** (2026-11-09): `V2__add_student_role.sql` (A4a entity ↔ A4c baseline çakışması), V1'ler idempotent + `baseline-version: 0` (temiz kurulumda servis sırası bağımsız), kütüphaneci parolası `DEMO_LIBRARIAN_PASSWORD`'e taşındı, `discovery.locator` kapalı, `/actuator` → `/actuator/health`, `FineSchemaInitializer` silindi.
- [x] **A4 merge + E2E** (2026-11-09): `main` `9c69a49`; 11 konteyner sağlıklı; yetkilendirme matrisi ve ceza zinciri canlı doğrulandı.
- [x] **`.beyin/` commit**, **`target/` temizliği** (482.9 MB), **`Turkcell-Java-Odev` WIP** (`cef1fa7` + `d597029`), **klasör gruplandırma** (2026-11-09).
