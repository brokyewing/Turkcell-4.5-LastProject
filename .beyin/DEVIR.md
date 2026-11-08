# Devir — Kitaplık (Turkcell bitirme)
**Son araç:** opencode (orkestra)
**Tarih:** 2026-11-09
**Durum:** A1+A2+A3 bitti ve `main`'e birleşti; uçtan uca akış gerçek konteynerlerde doğrulandı

## Nerede kaldık
- **Proje yolu değişti:** `Projelerim\Turkcell-egitimi\04-bitirme\LibraryApp-Last`
  (2026-11-09'da kök klasör konuya göre gruplandı; eski yol `Turkcell-Java-Odev\LibraryApp-Last`).
- `main` HEAD `2841116` — A1, A2, A3 birleşti, çakışma yok. Çalışma dalları duruyor:
  `a1-nefes`, `a2-frontend`, `a3-kurallar`. Worktree'ler kaldırıldı.
- **Merge sonrası ölçüm:** 8 modül `package` rc=0; testler → book 6, loan 15, fine 6, reservation 7, hepsi 0 fail.
- **Uçtan uca (gerçek konteynerler, port override ile):**
  - login `S1001/pass123` → 200 + JWT ✔
  - `GET /api/books/1` available 2 → ödünç al → **1** → iade → **2** ✔
  - geçmiş tarihli ödünç (10 gün) → iade → ceza **45.0 TL** (`LATE_RETURN`, 5 TL/gün) ✔ (Kafka→fine zinciri)
  - tüm kopyalar ödünçteyken yeni ödünç → **400 "CopyBook is already loaned"** ✔
  - tokensuz `GET /api/books` → 401, `GET /api/students` → 401 ✔
  - `POST /api/students` eksik gövde → 400, tam gövde → 200 + "Genel" bölümü ✔
- Test verileri silindi; `books.available_copies = total_copies` (3 kitap × 2), loan/fine tabloları boş.
- Not: ödünç tablosunun gerçek adı `studentcopybookloan` (`loans` değil); kitap kolonları
  `total_copies` / `available_copies`.

## Sıradaki adım
1. **A4 (vitrin/güvenlik):** JWT'ye rol claim + gateway yol-rol kontrolü; servis port publish'lerini
   kaldır; secret'ları env'e; Flyway bağla; README hizala.
   Kabul: tokensuz `/api/students` 401, öğrenci token'ıyla `DELETE /api/books/1` 403.
2. Tarayıcıda elle E2E: `frontend-taste` (`npm run dev`, proxy 18090) → giriş → ödünç → iade → ceza.
3. `FineSchemaInitializer` kalıcılaştır (şu an açılışta mükerrer `borrowid` siler + kısmi unique index kurar — geçici).
4. Reviewer'ın ertelenen düşük bulguları: commit-anı rollback telafisi için entegrasyon testi,
   `returnLoan` telafisini işlem kimliğine bağlama.

## Engeller
- Commit/push ve silme için kullanıcı onayı gerekiyor (Kapı 2). `main` yerel; `origin/main`'e push edilmedi.
- Lokal 8080 dolu (`wslrelay`): compose'u her zaman port override ile çalıştır
  (`C:\Users\asker\AppData\Local\Temp\opencode\override-a1.yml`, proje adı `kitaplik`).

## Dokunma
- `design-showcase/` — ürünün parçası değil, tasarım denemesi; silme kararı kullanıcıda.
- `k8s/` — tek servis manifest; A4'e kadar genişletilmiyor.
- `*.pem`, `.env` — secret.
- `%USERPROFILE%\.docker-java.properties` (`api.version=1.44`) — silinirse Testcontainers testleri kırılır.
