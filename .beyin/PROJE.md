# Proje — Kitaplık (Turkcell 4.5 Bitirme — LastProject)

**Durum:** çalışıyor — 11/11 konteyner healthy; yetkilendirme + sahiplik (IDOR) + ödünç/iade/ceza zinciri canlı doğrulandı.
Açık: CORS, frontend `role`, tarayıcı E2E, `main` push kararı.
**Tarih:** 2026-11-09
**Yol:** `C:\Users\asker\Projelerim\Turkcell-egitimi\04-bitirme\LibraryApp-Last`
**Depo:** https://github.com/brokyewing/Turkcell-4.5-LastProject (branch `main`, public)
**Üst proje:** `Turkcell-egitimi` (bu, onun içindeki bitirme teslimi — ayrı beyin)

## Ne yapıyoruz
Üniversite kütüphanesi yönetim sistemi: kitap/yazar/kategori/kopya kataloğu, üye (öğrenci + bölüm),
ödünç-verme/iade, rezervasyon, gecikme cezası. 8 Spring Boot 3.5.5 mikroservisi (Eureka + Gateway +
Config + 5 iş servisi), PostgreSQL, Redis (cache), Kafka (yalnız `loan.returned` olayı).
İki React/Vite ön yüz: `library-frontend` (admin paneli) ve `frontend-taste` (üye + kütüphaneci yüzeyi).

## Kapsam
- Servisler: book(8081), member(8083), loan(8082), reservation(8084), fine(8085), gateway(8080), discovery(8761), config(8888)
- Katalog: kitap, yazar, kategori, fiziksel kopya (`copybooks`), sayfalama, Redis cache (`getBookDtoById`)
- Üyelik: öğrenci, bölüm, JWT girişi (BCrypt, eski düz metni girişte yükseltme)
- Ödünç: Feign ile öğrenci + kopya doğrulama, iade, `loan.returned` yayını
- Ceza: Kafka consumer gecikme cezası (gün × 5 TL), elle ceza, ödeme
- Rezervasyon: oluştur / iptal
- Gateway: JWT imza doğrulaması (public: `/api/auth/`, swagger, actuator)
- Altyapı: Docker Compose (postgres+redis+kafka+8 servis), GitHub Actions matrix CI, `k8s/book-service.yaml` başlangıcı

## Kapsam dışı
- Bildirim (e-posta/SMS), ödeme entegrasyonu (ceza ödemesi yalnız durum değişimi)
- Kütüphaneci onay akışı, çok şubeli yapı

## Ölçülen durum (2026-11-09, A1–A4 + C1 + C2 sonrası)

### Derleme ve testler — hepsi yeşil
| Modül | Test |
|---|---|
| book | 6 |
| loan | 22 |
| fine | 9 |
| reservation | 9 |
| member | 7 |
| gateway | 23 |
| **toplam** | **76, 0 fail** |
| config-server, discovery-server | paket rc=0 (test yok) |

### Çalışma zamanı (gerçek konteynerler, `kitaplik` projesi, override-a4)
| Deneme | Sonuç |
|---|---|
| `docker compose up -d --build` | **11/11 healthy** |
| login `S1001/pass123` | 200 + JWT, `role=STUDENT` |
| login `L1001` / `DEMO_LIBRARIAN_PASSWORD` | 200 + JWT, `role=LIBRARIAN` |
| tokensuz `GET /api/books` / `/api/students` / `DELETE /api/books/1` | **401** |
| STUDENT: `GET /api/books` | 200 |
| STUDENT: `GET /api/students`, `DELETE/POST /api/books`, `GET /api/fines` | **403** |
| LIBRARIAN: `GET /api/students`, `GET /api/fines` | 200 |
| STUDENT + `X-Role: LIBRARIAN` spoof | hâlâ **403** |
| ödünç → iade | stok 2 → 1 → 2 |
| 10 gün gecikmeli iade | **50.0 TL** `LATE_RETURN` (5 TL/gün), Kafka→fine |
| Flyway | dolu DB: `Schema is up to date`; taze DB: her servis kendi V1'ini çalıştırır |

### Sahiplik (IDOR) matrisi — canlı, 19/19 OK
| Senaryo | Sonuç |
|---|---|
| STUDENT kendi: `students/1`, `loans/members/1`, `borrow/me/1`, `fines/students/1/total` | **200** |
| STUDENT başkası: aynı dört uç | **403** |
| STUDENT `POST /api/loans {studentId: 2}` / `POST /api/reservations {studentId: 2}` | **403** |
| STUDENT `GET /api/fines` (parametresiz), `GET /api/students` (liste) | **403** |
| LIBRARIAN aynı uçlara | **200** |
| STUDENT + `X-Role: LIBRARIAN` + `X-Student-Id: 2` → `GET /api/students/2` | **403** |
| STUDENT kendi ödüncü + iadesi | **200 / 200** |
| `GET /api/auth/../api/members`, `%2e%2e`, `;` varyantları | **403** (gateway reddediyor) |
| `GET /api/auth/tokens` (public önek değil) | **401** |

### Bilinen açıklar (ölçülmüş)
- **CORS yok:** gateway'de CORS/`globalcors` tanımı yok; tarayıcı preflight'ı 403.
- **Frontend `role` kullanmıyor:** `App.jsx` herkese kütüphaneci sekmesini gösteriyor.
- **Kabul edilmiş riskler (engelleyici değil):** `IdentityGuard` başlıksız çağrıyı iç çağrı sayar (fail-open,
  servis portları host'a kapalı); guard'sız `MemberController` + `@RequestBody Student` (LIBRARIAN-only bugün);
  403/404 varlık oracle'ı; `IdentityGuard` 4 kopya.

### Ön yüz
- `frontend-taste` gerçek API'ye bağlı (Page→dizi, kopya seçimi, ödünç/iade, ödenmemiş ceza toplamı); proxy 18090.
- `library-frontend` admin iskeleti: login + Page düzeltmesi yapıldı.

## Gereklilikler
| # | Gereklilik | Öncelik | Durum |
|---|---|---|---|
| G1 | `docker compose up` sonrası `POST /api/auth/login` 200 döner (seed'li kullanıcı) | yüksek | **✓** |
| G2 | book-service açılır, `books`/`copybooks` tabloları oluşur | yüksek | **✓** |
| G3 | Tarayıcıda login → kitap listesi → ödünç al → iade et akışı çalışır | yüksek | **~** (API ✓, CORS eksik) |
| G4 | Ödünçte stok düşer, iadede artar; aynı kopya iki kez verilemez | yüksek | **✓** |
| G5 | Rezervasyon fulfill + süre dolma akışı çalışır | orta | **✓** (7 test) |
| G6 | Yetkilendirme: öğrenci kitap silemez, üye listesini göremez; **başkasının kaynağına erişemez** | orta | **✓** (IDOR kapandı, 19/19 canlı) |
| G7 | config-server derlenir ve ayakta kalır; ölü `configurations/` yok | orta | **✓** (istemciye config sunmuyor — bilinçli) |
| G8 | Flyway bağlanır, şema migration'dan kurulur (`ddl-auto` yerine) | orta | **✓** |
| G9 | README/PROJE-HIKAYESI kodla çelişmez | düşük | kısmi |

## "Bitti" tanımı
- `docker compose up -d --build` → 11 konteyner healthy
- `POST /api/auth/login` (seed kullanıcı) → 200 + token
- Tokensuz `GET /api/books` → 401; tokenlı → 200 ve `content` dizisi
- Tarayıcıda ödünç al + iade et akışı ekran görüntüsüyle doğrulanır
- `mvn test` tüm modüllerde yeşil (Testcontainers dahil, Docker açıkken)

## Mimari kararlar
- **Tek veritabanı, tek şema** — servis başına şema ayrımı yok; her servis kendi
  `flyway_schema_history_<servis>` tablosunu kullanır, migration'lar idempotenttir (servis sırası bağımsız).
- **Feign + Kafka birlikte:** katalog/üye doğrulaması senkron (Feign), ceza tetikleme asenkron (Kafka). Ayrım doğru.
- **JWT'de rol var:** `role` claim (`STUDENT`/`LIBRARIAN`); gateway yol-rol allow-list uygular.
  **İki katmanlı sahiplik:** gateway path'teki `{id}`'yi token `studentId` claim'iyle karşılaştırır;
  servisler gövde/kayıt sahipliğini `IdentityGuard` ile doğrular (`X-Role: LIBRARIAN` serbest,
  `X-Role` yoksa iç Feign çağrısı sayılır, aksi halde `X-Student-Id` eşleşmeli).
- İki ön yüzden **`frontend-taste` ürün yüzeyi**, `library-frontend` demo/iskelet kabul edildi.
