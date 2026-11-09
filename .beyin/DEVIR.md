# Devir — Kitaplık (Turkcell bitirme)
**Son araç:** opencode (orkestra)
**Tarih:** 2026-11-09
**Durum:** A1–A4 kapandı ve `main`'e birleşti; 11 konteyner sağlıklı, yetkilendirme + ceza zinciri canlı doğrulandı

## Nerede kaldık
- **Proje yolu:** `Projelerim\Turkcell-egitimi\04-bitirme\LibraryApp-Last`
  (2026-11-09'da kök klasör konuya göre gruplandı; eski yol `Turkcell-Java-Odev\LibraryApp-Last`).
- `main` HEAD **`9c69a49`**. Çalışma dalları temizlendi (`a1..a4d` hepsi birleşti, silindi);
  yalnız `yedek` ve `main` kaldı. Worktree'ler kaldırıldı.
- **Merge sonrası testler:** book 6 · loan 15 · fine 6 · reservation 7 · member 3 · gateway 10 — hepsi 0 fail.
- **Uçtan uca (gerçek konteynerler, `kitaplik` projesi, override-a4 ile):**
  - 11/11 konteyner `healthy` (A4 öncesi member-service `Exited (1)` idi — A4d düzeltti).
  - login `S1001/pass123` → 200, `role=STUDENT`; `L1001/librarian123` → 200, `role=LIBRARIAN` ✔
  - Token'suz `GET /api/books`, `/api/students`, `DELETE /api/books/1` → **401** ✔
  - STUDENT: `GET /api/books` **200**; `GET /api/students` **403**; `DELETE/POST /api/books` **403**;
    `GET /api/fines` **403** ✔
  - LIBRARIAN: `GET /api/students` **200** (2 öğrenci), `GET /api/fines` **200** ✔
  - Header spoofing: STUDENT + `X-Role: LIBRARIAN` + `X-Student-Id: 1` → `GET /api/students` **403** ✔
  - Ödünç: `POST /api/loans` → 200; stok 2 → **1**; iade sonrası **2** ✔
  - Gecikmeli iade (10 gün) → **50.0 TL** `LATE_RETURN` → Kafka→fine zinciri `FineSchemaInitializer`
    **silinmiş hâlde** çalışıyor ✔
- Test verisi temizlendi: `fines` 0, `studentcopybookloan` 0, 3 kitap × `available_copies = total_copies`.

## Sıradaki adım
1. **IDOR (YÜKSEK):** gateway başlığı taşıyor ama servisler okumuyor → STUDENT başkasının
   `students/{id}` PII'sini, ödünçlerini, cezalarını okuyabiliyor. Sahiplik kontrolü ekle.
2. **CORS kararı + frontend `role`** — tarayıcı E2E'sini bunlar bloke ediyor.
3. Tarayıcıda elle E2E: `frontend-taste` (`npm run dev`, proxy 18090) → giriş → ödünç → iade → ceza.
4. Kullanıcı kararları: `configurations/` silinsin mi; `main` `origin`'e push edilsin mi;
   vault commit `fe31ff0` push edilsin mi.

## Engeller
- Commit/push ve silme için kullanıcı onayı gerekiyor (Kapı 2). `main` **yerel**, `origin/main`'e push edilmedi.
- Lokal **8080 dolu** (`wslrelay`): compose'u her zaman override ile çalıştır
  (`C:\Users\asker\AppData\Local\Temp\opencode\override-a4.yml`, proje adı `kitaplik`, gateway 18090).

## Dokunma
- `design-showcase/` — ürünün parçası değil, tasarım denemesi; silme kararı kullanıcıda.
- `configurations/` — **ölü**: hiçbir serviste `spring-cloud-starter-config` yok, `spring.config.import` yok.
- `*.pem`, `.env` — secret. `.env` gitignore'da; `DEMO_LIBRARIAN_PASSWORD` burada, repoda değil.
- `%USERPROFILE%\.docker-java.properties` (`api.version=1.44`) — silinirse Testcontainers testleri kırılır.
- `V1__baseline.sql` tablo tanımları canlı şemadan üretildi; yalnız `IF NOT EXISTS`/`DO` sarmalı eklenebilir.
