# Proje — Kitaplık (Turkcell 4.5 Bitirme — LastProject)

**Durum:** geliştirme — kod var, ürün çalışmıyor (kimse giriş yapamıyor, book-service açılmıyor)
**Tarih:** 2026-11-08
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
- Rol/yetki ayrımı (şu an tasarımda yok — Adım 4'te eklenecek)
- Bildirim (e-posta/SMS), ödeme entegrasyonu (ceza ödemesi yalnız durum değişimi)
- Kütüphaneci onay akışı, çok şubeli yapı

## Ölçülen durum (2026-11-08, bu dizinde)

### Derleme
| Modül | Sonuç |
|---|---|
| discovery, gateway, book, member, loan, reservation, fine | `mvn -DskipTests package` OK |
| **config-server** | **DERLENMİYOR** — `ConfigServerApplication.java:6` `EnableEurekaClient` bulunamıyor (Spring Cloud 2025.0.3'ten kaldırıldı) |

### Çalışma zamanı (postgres+redis+kafka ayağa kaldırılıp denendi)
| Deneme | Sonuç |
|---|---|
| `book-service` başlatma | **FAILED** — `jpaAuditingHandler` bean çakışması (`BookServiceApplication.java:10` + `config/JpaConfig.java:11` ikisi de `@EnableJpaAuditing`) |
| DB tabloları | yalnız 5: `students`, `deparmants`, `fines`, `reservations`, `studentcopybookloan` — **`books`/`authors`/`book_categories`/`copybooks` yok** |
| `GET /api/books` (tokensuz, gateway) | 401 ✓ beklenen |
| `POST /api/auth/login` (`S1001/pass123`) | 400 `Invalid credentials`; `students` tablosu **0 satır** (seed yok) |
| `POST /api/students` (tokensuz) | 401 → kayıt ucu token istiyor, giriş yapacak kullanıcı yok (**kilitli döngü**) |
| `GET /actuator/health` (gateway) | 200 ✓ |
| loan-service testleri | 9/9 PASS |
| book-service testi (Testcontainers) | Docker erişimi yoksa hata — CI'da Docker var, lokalde kırılgan |

### Ön yüz
- `library-frontend/src/api.js:7` hiç `Authorization` başlığı göndermiyor → gateway tüm ekranları 401'ler
- `library-frontend/src/data.jsx:21` `Page` gövdesini dizi sanıyor; `views.jsx:14` `books.reduce` → çalışma anında TypeError
- `frontend-taste` login + token taşıyor (daha ürün gibi) ama demo kullanıcı (`App.jsx:35`) hiç seed edilmiyor; ödünç/iade/ceza aksiyonu yok

## Gereklilikler
| # | Gereklilik | Öncelik | Durum |
|---|---|---|---|
| G1 | `docker compose up` sonrası `POST /api/auth/login` 200 döner (seed'li kullanıcı) | yüksek | yok |
| G2 | book-service açılır, `books`/`copybooks` tabloları oluşur | yüksek | yok |
| G3 | Tarayıcıda login → kitap listesi → ödünç al → iade et akışı çalışır | yüksek | yok |
| G4 | Ödünçte stok düşer, iadede artar; aynı kopya iki kez verilemez | yüksek | yok |
| G5 | Rezervasyon fulfill + süre dolma akışı çalışır | orta | yok |
| G6 | Yetkilendirme: öğrenci kitap silemez, üye listesini göremez | orta | yok |
| G7 | config-server derlenir ve servisler config client olur (ya da servis tamamen kaldırılır) | orta | yok |
| G8 | Flyway bağlanır, şema migration'dan kurulur (`ddl-auto` yerine) | orta | yok |
| G9 | README/PROJE-HIKAYESI kodla çelişmez | düşük | yok |

## "Bitti" tanımı
- `docker compose up -d --build` → 8 konteyner healthy
- `POST /api/auth/login` (seed kullanıcı) → 200 + token
- Tokensuz `GET /api/books` → 401; tokenlı → 200 ve `content` dizisi
- Tarayıcıda ödünç al + iade et akışı ekran görüntüsüyle doğrulanır
- `mvn test` tüm modüllerde yeşil (Testcontainers dahil, Docker açıkken)

## Mimari kararlar
- **Tek veritabanı, tek şema** — servis başına şema ayrımı yok. Kısa vadede kabul; Adım 4'te konuşulacak.
- **Feign + Kafka birlikte:** katalog/üye doğrulaması senkron (Feign), ceza tetikleme asenkron (Kafka). Ayrım doğru.
- **JWT'de rol yok** — gateway yalnız imza doğruluyor. Yetkilendirme G6'ya kadar yok sayılıyor.
- İki ön yüzden **`frontend-taste` ürün yüzeyi**, `library-frontend` demo/iskelet kabul edildi.
