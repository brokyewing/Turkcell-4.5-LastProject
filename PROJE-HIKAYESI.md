# Kitaplık — Proje Hikâyesi

> Bir kütüphane yönetim sistemi. Spring Boot mikroservisleriyle kurulmuş bir backend,
> iki ayrı yüzey: **kütüphaneci için admin paneli** ve **üyeler için okuma uygulaması**.

---

## 1. Problem: neden böyle bir sistem?

Bir üniversite kütüphanesi düşün. Elle yönetilirse:
- Hangi kitabın kaç kopyası var, hangisi kimde — kimse tam bilmez.
- Kitap geç getirilir, ceza hesaplamak angarya olur, çoğu zaman unutulur.
- Öğrenci "bende hangi kitaplar var, ne zaman iade etmeliyim?" diye kütüphaneye gelmek zorunda kalır.

**Çözüm:** her işi kaydeden, kuralları otomatik işleten, hem personelin hem öğrencinin
kendi ekranından yönetebildiği bir yazılım. İşte Kitaplık bu.

---

## 2. Sistem nasıl bölündü? (bounded context'ler)

Kütüphane işini gerçek departmanlarına göre böldük — her biri ayrı bir Spring Boot mikroservisi:

| Servis | Sorumluluğu | Port |
|---|---|---|
| **book-service** | Katalog: kitap, yazar, kategori, fiziksel kopya | 8081 |
| **member-service** | Üyelik: öğrenci, bölüm + **kimlik doğrulama (JWT)** | 8083 |
| **loan-service** | Ödünç: kim neyi ne zaman aldı/iade etti | 8082 |
| **fine-service** | Ceza: gecikince otomatik borç | 8085 |
| **reservation-service** | Rezervasyon: sıradaki kitap | 8084 |
| **gateway-server** | Tek giriş kapısı + JWT bekçisi | 8080 |
| **discovery-server** | Servis rehberi (Eureka) | 8761 |

Destek altyapısı: **PostgreSQL** (kalıcı veri), **Redis** (hız/cache), **Kafka** (servisler-arası olaylar).

---

## 3. Bir günün hikâyesi (senaryo)

**Sabah — Öğrenci Ali (üye uygulaması):**
1. Ali telefonundan Kitaplık uygulamasını açar, öğrenci numarası + parolayla **giriş yapar**.
   → member-service parolayı doğrular, ona bir **JWT** verir. Artık her isteği bu token'la gider.
2. Ana ekranda önerilen kitapları görür, "1984"ü arar, **ödünç alır**.
   → gateway token'ı doğrular → loan-service, member-service ve book-service'e
   "bu öğrenci ve bu kopya var mı?" diye sorar (Feign) → uygunsa ödünç kaydını açar.
3. Popüler kitaplar listesi anında gelir — çünkü **Redis'ten** okunuyor, her seferinde veritabanı yorulmuyor.

**İki hafta sonra — Ali kitabı geç getirir:**
4. Ali "1984"ü iade eder. loan-service ödüncü kapatır ve bir **olay yayınlar**: *"12 no'lu ödünç iade edildi, 16 gün gecikmeli."* (Kafka)
5. loan-service devam eder, kimseyi beklemez. **fine-service** bu olayı dinler,
   gecikmeyi görür ve **otomatik 80 TL ceza keser**. Ali'nin ekranında ceza belirir.
   → loan-service, ceza kesilip kesilmediğini bilmez bile (gevşek bağlılık).

**Öğleden sonra — Kütüphaneci Ayşe (admin paneli):**
6. Ayşe admin panelinde **dashboard**'u açar: kaç kitap, kaç üye, kaç açık ödünç,
   kaç ödenmemiş ceza — hepsi tek bakışta. Gecikmeli ödünçleri, son işlemleri görür.
7. Yeni gelen kitapları **ekler**, kopya sayısını günceller.
   → book-service kaydı güncelleyince Redis cache'ini de temizler, kimse bayat veri görmez.

Bu senaryonun her adımı, kurduğumuz backend'in bir parçasını çalıştırıyor:
JWT, Feign, Redis, Kafka, @Transactional, katmanlı mimari.

---

## 4. İki yüzey, iki tasarım dili

Aynı backend, iki farklı kitle → iki farklı frontend:

### A) Admin Paneli (Kütüphaneci)
- **Kim:** kütüphane personeli, gün boyu ekran başında, verimlilik ister.
- **Ne:** dashboard (KPI'lar + grafikler), veri tabloları, hızlı arama/filtre, kitap/üye/ceza yönetimi.
- **Tasarım dili:** yoğun, hızlı, "cockpit". *Spring'in çalıştığı kısım burada görünür* —
  servis durumları, canlı sayılar, işlem akışı.

### B) Üye Uygulaması (Öğrenci)
- **Kim:** öğrenci, telefonundan, ara sıra girer, keyifli bir deneyim ister.
- **Ne:** kitap keşfet, ödünç al, "kitaplarım", iade tarihleri, cezalarım — SaaS tarzı.
- **Tasarım dili:** ferah, sürükleyici, mobil-öncelikli. Kişisel kütüphane hissi.

> Aynı üründe iki farklı bağlam, iki farklı tasarım — doğru aracı doğru işe koymak.
> Bu ayrımı **taste-skill** ile kuracağız.

---

## 5. Teknik özet (CV/mülakat için tek bakış)

**Kitaplık — Mikroservis tabanlı kütüphane yönetim sistemi**
- Java 21, Spring Boot 3.5, Spring Cloud (Eureka discovery, Gateway, OpenFeign)
- 5 iş servisi, bounded-context'lere göre bölünmüş (DDD)
- PostgreSQL, **Redis** (Spring Cache ile önbellek), **Apache Kafka** (event-driven: otomatik ceza)
- **JWT** kimlik doğrulama (gateway'de merkezi bekçi), OpenAPI/Swagger
- Katmanlı mimari, DTO'larla entity sızıntısı önlendi, constructor injection, @Transactional
- **Testler:** JUnit 5 + Mockito (iş kuralları + servis-hatası senaryoları)
- **Docker Compose** ile tüm sistem tek komutla ayağa kalkar
- İki frontend: React admin paneli + üye SaaS uygulaması
