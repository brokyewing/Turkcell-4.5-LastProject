# LibraryApp-Last - Microservices Architecture

Bu proje, LibraryApp'in microservices mimarisine dönüştürülmüş versiyonudur. Tüm işlevsellik LibraryApp'teki gibi korunmuş, ancak mimari olarak microservices pattern'i kullanılmıştır.

## Proje Yapısı

```
LibraryApp-Last/
├── discovery-server/          # Eureka Server (Port: 8761)
├── gateway-server/            # Spring Cloud Gateway (Port: 8080)
├── config-server/             # Spring Cloud Config Server (Port: 8888)
│
├── book-service/              # Kitap, Yazar, Kategori yönetimi (Port: 8081)
├── member-service/            # Üye/Öğrenci yönetimi (Port: 8083)
├── loan-service/              # Ödünç alma/verme (Port: 8082)
├── reservation-service/       # Rezervasyon (Port: 8084)
├── fine-service/              # Ceza yönetimi (Port: 8085)
│
└── docker-compose.yml         # Tüm servisleri ayağa kaldırmak için
```

## Servisler

### 1. Discovery Server (Eureka)
- **Port:** 8761
- Tüm servislerin kayıt olduğu service registry

### 2. Gateway Server
- **Port:** 8080
- API Gateway - tüm istekler buradan geçer
- Route'lar:
  - `/api/books/**` → book-service
  - `/api/authors/**` → book-service
  - `/api/categories/**` → book-service
  - `/api/loans/**` → loan-service
  - `/api/members/**` → member-service
  - `/api/students/**` → member-service
  - `/api/departments/**` → member-service
  - `/api/reservations/**` → reservation-service
- `/api/fines/**` → fine-service
- `/api/copybooks/**` → book-service

### 3. Config Server
- **Port:** 8888
- Merkezi konfigürasyon yönetimi

### 4. Book Service
- **Port:** 8081
- Kitap, Yazar, Kategori ve CopyBook yönetimi
- Endpoints: `/api/books`, `/api/authors`, `/api/categories`, `/api/copybooks`

### 5. Member Service
- **Port:** 8083
- Öğrenci/Üye ve Bölüm yönetimi
- Endpoints: `/api/members`, `/api/students`, `/api/departments`

### 6. Loan Service
- **Port:** 8082
- Ödünç alma/verme işlemleri
- Endpoints: `/api/loans`, `/api/borrow`

### 7. Reservation Service
- **Port:** 8084
- Rezervasyon yönetimi
- Endpoints: `/api/reservations`

### 8. Fine Service
- **Port:** 8085
- Ceza yönetimi
- Endpoints: `/api/fines`

## Teknolojiler

- **Spring Boot 3.5.5**
- **Spring Cloud 2024.0.0**
- **Spring Cloud Netflix Eureka** (Service Discovery)
- **Spring Cloud Gateway** (API Gateway)
- **Spring Cloud Config** (Configuration Management)
- **PostgreSQL** (Database)
- **Java 21**
- **Maven**

## Gereksinimler

### Docker ile Çalıştırma İçin:
- **Docker** 20.10+
- **Docker Compose** 2.0+

### Manuel Kurulum İçin:
- Java 21
- Maven 3.6+
- PostgreSQL 14+

## Veritabanı

Tüm servisler aynı PostgreSQL veritabanını kullanabilir veya servis bazlı ayrı veritabanları kullanılabilir.

Veritabanı bağlantısı:
- **URL:** `jdbc:postgresql://localhost:5432/library_db`
- **Username:** `admin`
- **Password:** `admin`

## Çalıştırma

### 🐳 Docker ile Çalıştırma (Önerilen - Tek Komut)

**Tüm servisleri tek komutla başlatmak için:**

```bash
cd LibraryApp-Last
docker-compose up --build
```

**Arka planda çalıştırmak için:**
```bash
docker-compose up -d --build
```

**Servisleri durdurmak için:**
```bash
docker-compose down
```

**Logları görmek için:**
```bash
docker-compose logs -f
```

**Belirli bir servisin loglarını görmek için:**
```bash
docker-compose logs -f book-service
```

Docker Compose otomatik olarak:
- ✅ PostgreSQL veritabanını oluşturur ve başlatır
- ✅ Tüm servisleri doğru sırayla başlatır (bağımlılıklara göre)
- ✅ Servisler arası ağı kurar
- ✅ Health check'ler yapar


### 📋 Manuel Kurulum (Docker Olmadan)

#### 1. Veritabanını Hazırlayın
PostgreSQL'de `library_db` veritabanını oluşturun:

```sql
CREATE DATABASE library_db;
```

#### 2. Servisleri Başlatın

**Sıralama önemli!** Şu sırayla başlatın:

1. **Discovery Server:**
```bash
cd discovery-server
mvn spring-boot:run
```

2. **Config Server:**
```bash
cd config-server
mvn spring-boot:run
```

3. **Book Service:**
```bash
cd book-service
mvn spring-boot:run
```

4. **Member Service:**
```bash
cd member-service
mvn spring-boot:run
```

5. **Loan Service:**
```bash
cd loan-service
mvn spring-boot:run
```

6. **Reservation Service:**
```bash
cd reservation-service
mvn spring-boot:run
```

7. **Fine Service:**
```bash
cd fine-service
mvn spring-boot:run
```

8. **Gateway Server:**
```bash
cd gateway-server
mvn spring-boot:run
```

### 3. Servisleri Kontrol Edin

- **Eureka Dashboard:** http://localhost:8761
- **Gateway:** http://localhost:8080
- **API Örnekleri:**
  - http://localhost:8080/api/books
  - http://localhost:8080/api/members
  - http://localhost:8080/api/loans

## API Kullanımı

Tüm API istekleri Gateway üzerinden yapılır (Port: 8080):

```bash
# Kitap listesi
GET http://localhost:8080/api/books

# Yeni kitap ekle
POST http://localhost:8080/api/books
Content-Type: application/json
{
  "title": "Spring Boot Guide",
  "isbn": "1234567890123",
  "authorId": 1,
  "categoryId": 1,
  "totalCopies": 5,
  "availableCopies": 5
}

# Üye listesi
GET http://localhost:8080/api/members
```

## Özellikler

### ✅ Tamamlanan Özellikler
- ✅ **Microservices Mimari**: Her servis bağımsız çalışır
- ✅ **Service Discovery**: Eureka Server ile servis kayıt ve keşif
- ✅ **API Gateway**: Spring Cloud Gateway ile tek giriş noktası
- ✅ **Config Server**: Merkezi konfigürasyon yönetimi
- ✅ **Feign Client**: Servisler arası iletişim (OpenFeign)
- ✅ **Cross-Service Validation**: Servisler arası doğrulama
- ✅ **Global Exception Handling**: Her serviste merkezi hata yönetimi
- ✅ **CopyBook Controller**: Book-service'te copybook endpoint'leri

### 📝 Notlar

- Bu proje eğitim amaçlıdır ve production için bazı iyileştirmeler gerekebilir
- Circuit Breaker pattern eklenebilir (Resilience4j)
- Distributed tracing eklenebilir (Zipkin/Jaeger)
- Security (JWT, OAuth2) eklenebilir
- Response DTO'lara detaylı bilgiler eklenebilir (student name, book title vs.)

## LibraryApp ile Karşılaştırma

Bu proje, LibraryApp'in tüm özelliklerini içerir ancak microservices mimarisi kullanır:

- ✅ Kitap yönetimi
- ✅ Yazar yönetimi
- ✅ Kategori yönetimi
- ✅ Üye/Öğrenci yönetimi
- ✅ Ödünç alma/verme
- ✅ Rezervasyon
- ✅ Ceza yönetimi
- ✅ Bölüm yönetimi

## Lisans

