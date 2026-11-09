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
import java.nio.charset.StandardCharsets;
import java.util.Date;
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

    private MockServerWebExchange exchange(HttpMethod method, String path, String token) {
        return MockServerWebExchange.from(MockServerHttpRequest.method(method, path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build());
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
