# Dağıtım — Kitaplık

Bu belge yığını tek bir sunucuya (Oracle Always Free dahil) çıkarmanın yolunu anlatır.
Yerel geliştirme `docker-compose.yml` ile aynen devam eder; buradaki dosyalar onun
**üzerine binen** üretim katmanıdır.

## Neden ayrı katman

Ölçüm (11 konteyner, bu makinede): **boşta 2,85 GB**, 6 paralel işçi × 40 istek
yükü altında **tepe 3,0 GB**, 0 hata. Yani yığın küçük; sorun konfigürasyon:

| Sorun | Çözüm (bu katman) |
|---|---|
| Servislerde bellek limiti yok → JVM konteynerin %25'ini heap sanıp toplamda şişer | `mem_limit` + `-XX:MaxRAMPercentage=50` |
| `restart` politikası yok → sunucu yeniden başlarsa yığın kalkmaz | `restart: unless-stopped` |
| Dockerfile'lar imaj içinde Maven ile derliyor → ARM'da 20-30 dk + RAM | JAR'lar x86'da üretilir, `Dockerfile.runtime` yalnız çalıştırır (`arm64` hazır) |
| Gateway + altyapı portları dışa açık | hepsi kapatılır, tek kapı Caddy (80/443) |
| İki frontend ayrı origin → CORS | tek origin: `/` üye, `/admin/` yönetim, `/api` proxy |

Bellek bütçesi (limitlerin toplamı ≈ **7,4 GB**):

| Bileşen | Limit |
|---|---|
| postgres / kafka / redis | 768m / 1024m / 128m |
| discovery / config | 512m / 512m |
| gateway | 704m |
| 5 iş servisi | 768m × 5 |
| web (Caddy) | 128m |

Not: limit **heap + metaspace + code cache + thread stack**'ten büyük olmalı.
`MaxRAMPercentage=50` + `MaxMetaspaceSize=192m` ile 448m sınırı %93 doluyordu
(224 + 192 = 416/448) → çekirdek servisler 512m'ye çıkarıldı.

12 GB'lık bir sunucuda ~4,6 GB boşluk kalır. 8 GB'lık bir VPS'te de çalışır ama
`book/member/loan/reservation/fine` limitlerini 640m'ye, kafka'yı 768m'ye indir.

## Oracle Always Free gerçeği (2026)

- Always Free Ampere A1 **2 OCPU / 12 GB** (Haziran 2026'da 4/24'ten yarıya indirildi,
  18 Ağustos 2026'dan beri zorunlu). "8/16 GB ücretsiz" diye bir şey yok.
- 4 OCPU / 24 GB yalnız Pay-As-You-Go hesapta ücretsiz kalabiliyor — garanti değil.
- **ARM instance açmakta "Out of host capacity" sık görülür.** Farklı availability
  domain dene veya birkaç gün sonra tekrar dene.
- 200 GB blok depolama yeterli (imajlar ~5-6 GB, veri küçük).

## 1) JAR'ları üret (geliştirici makinesinde, x86)

```powershell
$mods = "discovery-server","config-server","gateway-server","book-service",
        "member-service","loan-service","reservation-service","fine-service"
foreach ($m in $mods) {
  Push-Location $m; mvn -B -q -DskipTests package; Pop-Location
}
Get-ChildItem *\target\*.jar | Select-Object FullName, Length
```

Her servis için **tek** `*.jar` olmalı (Spring Boot repackage çıktısı).

## 2) Sunucuya kopyala

Yalnız gerekenler: `docker-compose.yml`, `docker-compose.prod.yml`,
`Dockerfile.runtime`, `deploy/`, `.dockerignore`, `.env.prod.example`, iki frontend
klasörü (`node_modules` ve `dist` hariç) ve her servisin `*/target/*.jar` dosyası.

```powershell
# Kök dosyalar. (rsync varsa daha hızlı; Windows'ta scp yeterli.)
scp docker-compose.yml docker-compose.prod.yml Dockerfile.runtime .dockerignore `
    .env.prod.example ubuntu@SUNUCU:~/kitaplik/
scp -r deploy frontend-taste library-frontend ubuntu@SUNUCU:~/kitaplik/
```

**JAR'lar kendi klasörlerine gitmeli.** `scp */target/*.jar ...` hepsini TEK dizine
düzleştirir → `Dockerfile.runtime`'daki `COPY ${SERVICE}/target/*.jar` bulamaz ve
build kırılır. Doğrusu, servis başına bir döngü:

```powershell
$mods = "discovery-server","config-server","gateway-server","book-service",
        "member-service","loan-service","reservation-service","fine-service"
foreach ($m in $mods) {
  ssh ubuntu@SUNUCU "mkdir -p ~/kitaplik/$m/target"
  scp "$m/target/*.jar" "ubuntu@SUNUCU:~/kitaplik/$m/target/"
}
```

## 3) Ortam dosyası

```bash
cd ~/kitaplik
cp .env.prod.example .env.prod
chmod 600 .env.prod
# POSTGRES_PASSWORD, SPRING_DATASOURCE_PASSWORD, JWT_SECRET doldur.
# DEMO_LIBRARIAN_PASSWORD: üretimde BOŞ bırak (demo hesabı hiç seed edilmez).
# DOMAIN=app.example.com   (şemasız → otomatik HTTPS; port YAZMA)
```

Secret üretimi: `openssl rand -base64 48` (JWT), `openssl rand -base64 24` (DB).

## 4) Ayağa kaldır

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml \
  --env-file .env.prod up -d --build
docker compose -f docker-compose.yml -f docker-compose.prod.yml \
  --env-file .env.prod ps
```

Gereksinim: **Docker Compose ≥ 2.24** — `docker-compose.prod.yml` portları ve
`container_name`'leri `!reset` ile kapatıyor; eski sürümlerde YAML hatası alınır
(`docker compose version` ile kontrol et).

İlk açılışta Flyway şemayı kurar, seed verisini girer (~60-90 sn).

## 5) Doğrulama

```bash
curl -fsS https://app.example.com/ | head -5           # SPA HTML
curl -fsS https://app.example.com/admin/ | head -5     # yönetim paneli HTML
curl -fsS https://app.example.com/api/books            # 401 (token yok) — beklenen
# Olmayan bir varlık 404 dönmeli, 200 + HTML DEĞİL:
curl -s -o /dev/null -w '%{http_code} %{content_type}\n' https://app.example.com/assets/yok.js
T=$(curl -s -X POST https://app.example.com/api/auth/login \
      -H 'Content-Type: application/json' \
      -d '{"studentNumber":"S1001","password":"pass123"}' | jq -r .token)
curl -fsS -H "Authorization: Bearer $T" https://app.example.com/api/books?size=1
```

Tarayıcıda: `/` → öğrenci girişi (ödünç/iade), `/admin/` → yalnız kütüphaneci
(öğrenci girerse "Yönetim paneline erişim yok").

## 6) Güvenlik kontrol listesi

- [ ] `.env.prod` repoda **değil** (`.gitignore`'da), izinleri `chmod 600`
- [ ] `JWT_SECRET`, DB parolası, `DEMO_LIBRARIAN_PASSWORD` varsayılan **değil**
- [ ] Firewall: yalnız 22 (kısıtlı IP), 80, 443 açık
- [ ] `docker compose ... ps` → 12 servis, dışarıya yalnız `web`
- [ ] `DOMAIN` gerçek alan adı → sertifika otomatik; HTTPS'e yönlendirme Caddy'de
- [ ] HTTPS'te HSTS başlığı var (`curl -sI https://... | grep -i strict`)
- [ ] Olmayan varlık 404 dönüyor (200 + HTML değil): `/assets/yok.js`
- [ ] `DEMO_LIBRARIAN_PASSWORD` boş bırakıldıysa kütüphaneci hiç seed edilmez (tercih)

## 7) Bakım

```bash
# loglar (json-file, servis başına 10 MB × 3 dosya ile sınırlı)
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod logs -f gateway-server

# yeni sürüm: jar'ı kopyala → yalnız o servisi yeniden derle
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d --build book-service

# yedek (en kritik tek şey)
docker exec $(docker compose ... ps -q postgres) pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" > yedek.sql
```

## Geri alma

İmajlar `:latest` etiketiyle üretildiği için `up -d --build` etiketi **üzerine yazar**;
bu yüzden `up -d --no-build` eski sürümü DEĞİL, o an duran (yeni) imajı çalıştırır.
Doğru yol: her dağıtımda imajları sürümle etiketle, geri dönüşte etiketi göster.

```bash
# dağıtımdan ÖNCE mevcut sürümü sakla
docker tag kitaplik-prod-book-service:latest kitaplik-prod-book-service:2026-11-12

# geri dönüş (ör. tek servis)
docker tag kitaplik-prod-book-service:2026-11-12 kitaplik-prod-book-service:latest
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod \
  up -d --no-build book-service
```

Veritabanı şeması Flyway ile ileri yönlüdür; geri dönüş gerekiyorsa yedekten dönülür.

## Notlar / bilinçli tercihler

- `web` healthcheck yalnız Caddy yönetim API'sini (`127.0.0.1:2019`) yoklar; gateway'in
  kendi healthcheck'i var ve `web` ona `condition: service_healthy` ile bağlı.
- Taban imajlar yüzen etiketli (`caddy:2-alpine`, `postgres:14`). Sıkı tekrarlanabilirlik
  gerekirse digest'e sabitle (`image: caddy@sha256:...`).
- `stop_grace_period` varsayılan 10 sn. Uzun süren isteklerde kesme görülürse JVM
  servislerine `stop_grace_period: 30s` ekle.
- HSTS yalnız HTTPS'te gönderilir (Caddyfile'da `{scheme} == "https"` koşulu);
  yerel HTTP denemesinde tarayıcı kilitlenmez.
