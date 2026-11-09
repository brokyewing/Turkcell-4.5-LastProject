package com.turkcell.libraryapp.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Gateway'e gelen HER isteği süzer. Public yollar (giriş, dokümantasyon, sağlık, kayıt)
 * token istemez; diğer her şey geçerli bir "Bearer <JWT>" başlığı ister.
 * Token geçerliyse rolüne göre yetkilendirir: LIBRARIAN her yola, STUDENT yalnız
 * aşağıdaki allow-list'e erişebilir. Yetkisiz istek 403 ile, arkadaki servise HİÇ ulaşmadan döner.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/", "/swagger", "/v3/api-docs", "/actuator");

    // Kayıt ucu (yalnız POST) token istemez; GET/PUT/DELETE /api/students... hâlâ korumalı.
    private static final String PUBLIC_STUDENTS_PATH = "/api/students";

    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_LIBRARIAN = "LIBRARIAN";

    // Öğrenciye açık GET katalog kökleri (tüm alt yollarıyla).
    private static final List<String> STUDENT_GET_CATALOG_PATHS = List.of(
            "/api/books", "/api/authors", "/api/categories", "/api/copybooks");

    // GET /api/fines/students/{id}/total — yalnız toplam ceza deseni, başka fine yolu değil.
    private static final Pattern STUDENT_GET_FINE_TOTAL =
            Pattern.compile("^/api/fines/students/[^/]+/total$");

    // GET /api/reservations/{id} — liste (/api/reservations) DEĞİL, tek kayıt.
    private static final Pattern STUDENT_GET_RESERVATION =
            Pattern.compile("^/api/reservations/[^/]+$");

    // POST /api/reservations/{id}/cancel
    private static final Pattern STUDENT_POST_RESERVATION_CANCEL =
            Pattern.compile("^/api/reservations/[^/]+/cancel$");

    private final SecretKey key;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret) {
        // member-service ile AYNI gizli anahtar → orada imzalanan token burada doğrulanabilir.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();

        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }
        // Yalnız POST /api/students (kayıt) public; path tam eşleşmeli ki
        // /api/students/5 gibi alt yollar da korumalı kalsın.
        if (HttpMethod.POST.equals(method) && PUBLIC_STUDENTS_PATH.equals(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "Missing Bearer token");
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            // İmza + son kullanma tarihini doğrular; geçersizse exception fırlatır.
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        // Eski token'larda role yoksa öğrenci say (en az yetkili varsayılan).
        String role = claims.get("role", String.class);
        if (role == null) {
            role = ROLE_STUDENT;
        }

        if (!ROLE_LIBRARIAN.equals(role) && !isStudentAllowed(method, path)) {
            return error(exchange, HttpStatus.FORBIDDEN, "Access denied for role " + role);
        }

        return chain.filter(withTrustedIdentity(exchange, claims, role));
    }

    /**
     * Header spoofing'i engeller: istemcinin gönderdiği X-Student-Id / X-Role başlıkları
     * SİLİNİR, yerine yalnızca doğrulanmış token'dan türetilenler yazılır.
     */
    private ServerWebExchange withTrustedIdentity(ServerWebExchange exchange, Claims claims, String role) {
        Object studentId = claims.get("studentId");
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-Student-Id");
                    headers.remove("X-Role");
                    if (studentId != null) {
                        headers.set("X-Student-Id", String.valueOf(studentId));
                    }
                    headers.set("X-Role", role);
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    /**
     * STUDENT rolü için allow-list. Burada olmayan her (yöntem, yol) çifti 403 alır.
     */
    private boolean isStudentAllowed(HttpMethod method, String path) {
        if (HttpMethod.GET.equals(method)) {
            return STUDENT_GET_CATALOG_PATHS.stream().anyMatch(root -> matchesPrefix(path, root))
                    // /api/students/{id} — detay serbest, tam /api/students (liste) YASAK.
                    || path.startsWith("/api/students/")
                    || matchesPrefix(path, "/api/loans/members")
                    || matchesPrefix(path, "/api/borrow/me")
                    || STUDENT_GET_FINE_TOTAL.matcher(path).matches()
                    || STUDENT_GET_RESERVATION.matcher(path).matches();
        }
        if (HttpMethod.POST.equals(method)) {
            return path.equals("/api/loans")
                    || path.equals("/api/loans/return")
                    || path.equals("/api/reservations")
                    || STUDENT_POST_RESERVATION_CANCEL.matcher(path).matches();
        }
        return false;
    }

    /** path ya kökün kendisi ya da kökün altındaki bir yol olmalı (/api/books, /api/books/5). */
    private boolean matchesPrefix(String path, String root) {
        return path.equals(root) || path.startsWith(root + "/");
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // Eski davranışı bozmayan yardımcı başlık; asıl bilgi artık gövdede.
        response.getHeaders().add("X-Auth-Error", message);
        String body = "{\"message\":\"" + message.replace("\"", "\\\"")
                + "\",\"status\":" + status.value() + "}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // route'lamadan önce çalışsın
    }
}
