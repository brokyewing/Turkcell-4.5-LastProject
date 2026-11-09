# Devir — Kitaplık (Turkcell bitirme)
**Son araç:** opencode (orkestra)
**Tarih:** 2026-11-09
**Durum:** A1–A4 + C1 + C2 kapandı ve `main`'e birleşti; 11 konteyner sağlıklı;
IDOR kapatıldı ve canlıda 19/19 sahiplik senaryosu doğrulandı

## Nerede kaldık
- **Proje yolu:** `Projelerim\Turkcell-egitimi\04-bitirme\LibraryApp-Last`
- `main` HEAD **`3ac67bd`** (C1+C2 merge `2c8a334` + gateway sertleştirme `3038339` + test sıkılaştırma `3ac67bd`).
  `origin/main` hâlâ **`ce3d349`** — **push edilmedi** (kullanıcı onayı bekliyor).
- Çalışma dalları temizlendi (`c1-config`, `c2-idor` birleşti ve silindi); worktree yok.
- **Testler (birleşik hâl, `mvn -B clean test`):** book 6 · loan 22 · fine 9 · reservation 9 ·
  member 7 · gateway 27 = **80 test, 0 fail**.
- **Uçtan uca (gerçek konteynerler, `kitaplik` projesi + override-a4, gateway 18090):**
  - 11/11 konteyner `healthy`; login `S1001` → STUDENT, `L1001` → LIBRARIAN ✔
  - **C2 IDOR matrisi 19/19 OK** (`%TEMP%\opencode\e2e-c2.ps1`):
    - STUDENT kendi kaynakları (students/1, loans/members/1, borrow/me/1, fines/students/1/total) → **200**
    - STUDENT başkasının aynı uçları → **403**; `POST /api/loans {studentId:2}` → **403**;
      `POST /api/reservations {studentId:2}` → **403**; `GET /api/fines` (parametresiz) → **403**
    - LIBRARIAN aynı uçlara → **200**; header spoof (STUDENT + `X-Role: LIBRARIAN`) → **403**
    - gerçek akış bozulmadı: kendi ödüncü **200**, iadesi **200**
  - **Path traversal kapandı** (yeni gateway): `/api/auth/../api/members` → **403**,
    `%2e%2e` → **403**, `;` → **403**; `/api/auth/tokens` → **401** (public artık tam eşleşme)
  - C1: config-server `classpath:/` ile ayağa kalkıyor, health 200; compose'da `configurations` geçmiyor.
- Test verisi temizlendi: `studentcopybookloan` 0, `fines` 0, `reservations` 0,
  3 kitap × `available_copies = total_copies`.

## Yeni (C2) — nasıl çalışıyor
- **Gateway** (`JwtAuthenticationFilter`): STUDENT için path'teki `{id}` token `studentId` claim'ine eşit olmalı
  (`/api/students/{id}`, `/api/loans/members/{id}`, `/api/borrow/me/{id}`, `/api/fines/students/{id}/total`).
  `/api/students/{id}/exists` regex'e uymaz → öğrenciye kapalı, Feign iç çağrısı bozulmaz.
- **Servisler** (member/loan/reservation/fine): `security/IdentityGuard` —
  `X-Role: LIBRARIAN` serbest; `X-Role` yok = servisler arası (Feign) çağrı, serbest;
  `X-Student-Id` varsa sahiplik zorunlu. İhlal → `ForbiddenException` → `ExceptionDetails` gövdesiyle **403**.
- **Gateway sertleştirme (`3038339` + `3ac67bd`)**: public yol tam eşleşme (`/api/auth/login`), public dalda da
  `X-Role`/`X-Student-Id` silinir, `../` `//` `;` `\` `%2e` `%2f` içeren yol **403**.

## Sıradaki adım
1. **Kullanıcı kararı: `main` (`3ac67bd`) `origin`'e push edilsin mi?** (7 commit ileride)
2. **CORS** (ORTA): gateway'de CORS yok → tarayıcıdan ayrı origin'deki frontend preflight'ı 403.
   Karar: frontend'i aynı origin'e ters-proxy mi, gateway'e `globalcors` mu?
3. **Frontend `role` kullanmıyor** (ORTA): `LoginResponse.role` geliyor ama `frontend-taste/App.jsx`
   herkese "Kütüphaneci" sekmesini gösteriyor.
4. Tarayıcıda elle E2E (2 ve 3 bitince).
5. Vault commit `fe31ff0` push edilsin mi (Obsidian).

## Kalan bilinen riskler (kabul edilmiş, engelleyici değil)
- `IdentityGuard` başlıksız çağrıyı "iç çağrı" sayar (fail-open). Servis portları host'a kapalı;
  ileride NetworkPolicy / paylaşılan iç secret eklenmeli.
- `MemberController` (`/api/members`) guard'sız ve `@RequestBody Student` bağlıyor (rol/parola mass-assignment).
  Bugün gateway'de LIBRARIAN-only; sertleştirme hedefi.
- loan/return/reservation/fine'da "kaydı getir → sonra kontrol" sırası 403/404 ile varlık oracle'ı verebilir.
- `IdentityGuard` 4 serviste kopya (davranış birebir aynı); ortak modüle çıkarılabilir.

## Engeller
- Lokal **8080 dolu** (`wslrelay`): compose'u her zaman override ile çalıştır
  (`C:\Users\asker\AppData\Local\Temp\opencode\override-a4.yml`, proje `kitaplik`, gateway 18090).
- `kitaplik` projesinde Postgres kullanıcısı **`admin`**, DB **`library_db`** (`postgres` rolü yok).

## Dokunma
- `design-showcase/` — ürünün parçası değil, tasarım denemesi; silme kararı kullanıcıda.
- `*.pem`, `.env` — secret. `.env` gitignore'da; `DEMO_LIBRARIAN_PASSWORD` burada, repoda değil.
- `%USERPROFILE%\.docker-java.properties` (`api.version=1.44`) — silinirse Testcontainers testleri kırılır.
- `V1__baseline.sql` tablo tanımları canlı şemadan üretildi; yalnız `IF NOT EXISTS`/`DO` sarmalı eklenebilir.
- `config-server` **kalıyor** (8 servislik mimarinin parçası) ama istemciye yapılandırma sunmuyor;
  `native` profili silinirse varsayılan git sağlayıcısına düşüp patlar.
