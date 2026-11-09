package com.turkcell.libraryapp.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gateway yetkilendirme kuralları: public yollar, tokensuz 401, STUDENT allow-list ve
 * LIBRARIAN tam erişim. Gerçek imzalı token üretilir; mock exchange üzerinden süzülür.
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "library-app-super-secret-key-change-in-production-please-256bit!!";

    private SecretKey key;
    private JwtAuthenticationFilter filter;
    private AtomicBoolean downStreamCalled;
    private AtomicReference<ServerWebExchange> forwarded;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        filter = new JwtAuthenticationFilter(SECRET);
        downStreamCalled = new AtomicBoolean(false);
        forwarded = new AtomicReference<>();
        chain = exchange -> {
            downStreamCalled.set(true);
            forwarded.set(exchange);
            return Mono.empty();
        };
    }

    @Test
    void publicYolTokenOlmadanGecer() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login").build());

        filter.filter(exchange, chain).block();

        assertTrue(downStreamCalled.get(), "public yol zincire devam etmeliydi");
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void tokenYoksa401Doner() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/books").build());

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertFalse(downStreamCalled.get());
        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        assertTrue(body.contains("\"status\":401"), "gövde JSON olmalı: " + body);
    }

    @Test
    void studentDeleteBooks403Alir() {
        MockServerWebExchange exchange = exchange(HttpMethod.DELETE, "/api/books/1", token("STUDENT", 7L));

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(downStreamCalled.get());
        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        assertTrue(body.contains("\"status\":403"), "gövde JSON olmalı: " + body);
    }

    @Test
    void studentGetBooksGecer() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/books", token("STUDENT", 7L));

        filter.filter(exchange, chain).block();

        assertTrue(downStreamCalled.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void studentGetStudentsListesi403Alir() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/students", token("STUDENT", 7L));

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(downStreamCalled.get());
    }

    @Test
    void studentGetStudentDetayiGecer() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/students/7", token("STUDENT", 7L));

        filter.filter(exchange, chain).block();

        assertTrue(downStreamCalled.get(), "GET /api/students/{id} öğrenciye açık olmalı");
    }

    @Test
    void librarianDeleteBooksGecer() {
        MockServerWebExchange exchange = exchange(HttpMethod.DELETE, "/api/books/1", token("LIBRARIAN", 1L));

        filter.filter(exchange, chain).block();

        assertTrue(downStreamCalled.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void istemciBasliklariEzilemez() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/books")
                .header("X-Student-Id", "999")          // spoof denemesi
                .header("X-Role", "LIBRARIAN")          // spoof denemesi
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("STUDENT", 7L))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertTrue(downStreamCalled.get());
        HttpHeaders forwardedHeaders = forwarded.get().getRequest().getHeaders();
        assertEquals("7", forwardedHeaders.getFirst("X-Student-Id"), "token'daki id yazılmalı");
        assertEquals("STUDENT", forwardedHeaders.getFirst("X-Role"), "token'daki rol yazılmalı");
    }

    // role claim'i String değilse (dizi) 500 yerine STUDENT davranışı: liste 403.
    @Test
    void roleClaimDiziIseStudentSayilir() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/students")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenWithRoleClaim(List.of("LIBRARIAN"), 7L))
                        .build());

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(downStreamCalled.get());
    }

    // role claim'i sayıysa da STUDENT: izinli yol geçer, X-Role STUDENT olarak iletilir.
    @Test
    void roleClaimSayiIseStudentSayilir() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/books")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithRoleClaim(123, 7L))
                        .build());

        filter.filter(exchange, chain).block();

        assertTrue(downStreamCalled.get());
        assertEquals("STUDENT", forwarded.get().getRequest().getHeaders().getFirst("X-Role"));
    }

    // ---- C2: path id'si öğrencinin kendisine ait olmalı ----

    @Test
    void studentBaskaOgrenciDetayi403Alir() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/students/2", token("STUDENT", 1L));

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(downStreamCalled.get());
        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        assertTrue(body.contains("not your resource"), "gövde: " + body);
    }

    @Test
    void studentKendiDetayiGecer() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/students/1", token("STUDENT", 1L));

        filter.filter(exchange, chain).block();

        assertTrue(downStreamCalled.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void studentBaskaUyeOdencleri403Alir() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/loans/members/2", token("STUDENT", 1L));

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(downStreamCalled.get());
    }

    @Test
    void studentBaskaCezaToplami403Alir() {
        MockServerWebExchange exchange =
                exchange(HttpMethod.GET, "/api/fines/students/2/total", token("STUDENT", 1L));

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(downStreamCalled.get());
    }

    @Test
    void librarianBaskaOgrenciDetayiGecer() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/students/2", token("LIBRARIAN", 1L));

        filter.filter(exchange, chain).block();

        assertTrue(downStreamCalled.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    // /api/students/{id}/exists Feign iç çağrısıdır; gateway'den öğrenciye açılmaz.
    @Test
    void studentStudentExistsYolu403Alir() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/students/1/exists", token("STUDENT", 1L));

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(downStreamCalled.get());
    }

    // studentId claim'i olmayan STUDENT token'ı id taşıyan yola giremez.
    @Test
    void studentIdClaimsizToken403Alir() {
        Date now = new Date();
        String tokenWithoutStudentId = Jwts.builder()
                .subject("test-user")
                .claim("role", "STUDENT")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(key)
                .compact();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/students/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutStudentId)
                        .build());

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(downStreamCalled.get());
    }

    @Test
    void studentKendiUyeOdencleriGecer() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/loans/members/1", token("STUDENT", 1L));

        filter.filter(exchange, chain).block();

        assertTrue(downStreamCalled.get());
    }

    // ---- Sertleştirme: public yol tam eşleşme, başlık temizliği, bozuk yol reddi ----

    @Test
    void authOnekliBaskaYolPublicSayilmaz() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/tokens").build());

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertFalse(downStreamCalled.get());
    }

    @Test
    void publicYoldaDaIstemciBasliklariSilinir() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login")
                        .header("X-Role", "LIBRARIAN")
                        .header("X-Student-Id", "999")
                        .build());

        filter.filter(exchange, chain).block();

        assertTrue(downStreamCalled.get());
        HttpHeaders forwardedHeaders = forwarded.get().getRequest().getHeaders();
        assertNull(forwardedHeaders.getFirst("X-Role"), "public yolda X-Role sızmamalı");
        assertNull(forwardedHeaders.getFirst("X-Student-Id"), "public yolda X-Student-Id sızmamalı");
    }

    @Test
    void kayitUcundaDaIstemciBasliklariSilinir() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/students")
                        .header("X-Role", "LIBRARIAN")
                        .build());

        filter.filter(exchange, chain).block();

        assertTrue(downStreamCalled.get());
        assertNull(forwarded.get().getRequest().getHeaders().getFirst("X-Role"));
    }

    @Test
    void noktaNoktaIcErenYol403Alir() {
        MockServerWebExchange exchange = exchangeWithPath("/api/auth/../api/members");

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(downStreamCalled.get());
    }

    @Test
    void yuzdeKodluNoktaIcerenYol403Alir() {
        MockServerWebExchange exchange = exchangeWithPath("/api/auth/%2e%2e/api/members");

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(downStreamCalled.get());
    }

    @Test
    void yuzdeKodluEgikCizgiIcerenYol403Alir() {
        MockServerWebExchange exchange = exchangeWithPath("/api/books/a%2fb");

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(downStreamCalled.get());
    }

    @Test
    void ciftEgikCizgiIcerenYol403Alir() {
        MockServerWebExchange exchange = exchangeWithPath("//api/students");

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(downStreamCalled.get());
    }

    @Test
    void noktaliVirgulIcerenYol403Alir() {
        MockServerWebExchange exchange = exchangeWithPath("/api/books;jsessionid=abc");

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(downStreamCalled.get());
    }

    // Gerçek bir URI ile gelirse Spring/Netty yolu normalize edebilir; normalize olup
    // korumalı bir yola dönüşse bile istek kimliksiz geçmemeli (401/403), downstream'e varmamalı.
    @Test
    void kodluNoktaIcerenGercekUriKimliksizGecmez() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/%2e%2e/api/members").build());

        filter.filter(exchange, chain).block();

        org.springframework.http.HttpStatusCode status = exchange.getResponse().getStatusCode();
        assertNotNull(status);
        assertTrue(status.value() == 401 || status.value() == 403,
                "401 veya 403 beklenirdi, gelen: " + status);
        assertFalse(downStreamCalled.get());
    }

    // ---- CORS preflight ----

    @Test
    void preflightOptionsTokensuzGecer() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/books")
                        .header("Origin", "http://localhost:5174")
                        .header("Access-Control-Request-Method", "GET")
                        .build());

        filter.filter(exchange, chain).block();

        assertTrue(downStreamCalled.get(), "OPTIONS preflight zincire devam etmeliydi");
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void preflightOptionsKimlikBasligiTasimaz() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/students/2")
                        .header("Origin", "http://localhost:5174")
                        .header("Access-Control-Request-Method", "GET")
                        .header("X-Role", "LIBRARIAN")
                        .header("X-Student-Id", "999")
                        .build());

        filter.filter(exchange, chain).block();

        assertTrue(downStreamCalled.get());
        HttpHeaders forwardedHeaders = forwarded.get().getRequest().getHeaders();
        assertNull(forwardedHeaders.getFirst("X-Role"));
        assertNull(forwardedHeaders.getFirst("X-Student-Id"));
    }

    /** Yolu ham hâliyle taşıyan exchange (yüzde kodlamayı iki kez kodlamaz). */
    private MockServerWebExchange exchangeWithPath(String path) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, URI.create("http://localhost" + path)).build());
    }

    private MockServerWebExchange exchange(HttpMethod method, String path, String token) {        return MockServerWebExchange.from(MockServerHttpRequest.method(method, path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build());
    }

    private String tokenWithRoleClaim(Object roleClaim, Long studentId) {
        Date now = new Date();
        return Jwts.builder()
                .subject("test-user")
                .claim("studentId", studentId)
                .claim("role", roleClaim)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(key)
                .compact();
    }

    private String token(String role, Long studentId) {
        Date now = new Date();
        return Jwts.builder()
                .subject("test-user")
                .claim("studentId", studentId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(key)
                .compact();
    }
}
