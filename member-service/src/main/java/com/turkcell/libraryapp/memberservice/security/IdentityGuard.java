package com.turkcell.libraryapp.memberservice.security;

import com.turkcell.libraryapp.memberservice.exception.ForbiddenException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Gateway'in token'dan türettiği X-Role / X-Student-Id başlıklarına dayanan sahiplik kontrolü.
 *
 * <p>Kural: LIBRARIAN her şeyi yapar; geçerli X-Internal-Token varsa istek servisler arası (Feign)
 * sayılır ve serbesttir; aksi halde X-Student-Id varsa ve rol STUDENT ise kaynağın sahibi olmak
 * zorunludur.
 *
 * <p>Fail-closed: ne rol ne de geçerli internal token varsa istek reddedilir. internal.token
 * boşsa hiçbir servis içi çağrı kabul EDİLMEZ.
 *
 * <p>İstek, RequestContextHolder üzerinden okunur; böylece controller'lar request-scoped proxy'ye
 * bağlı kalmaz (standalone MockMvc testleri de çalışır).
 */
@Component
public class IdentityGuard {

    public static final String HEADER_ROLE = "X-Role";
    public static final String HEADER_STUDENT_ID = "X-Student-Id";
    public static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";
    private static final String ROLE_LIBRARIAN = "LIBRARIAN";
    private static final Logger log = LoggerFactory.getLogger(IdentityGuard.class);

    /** Beklenen servis içi paylaşılan sır; boşsa null (internal çağrı kabul edilmez). */
    private final byte[] expectedInternalToken;

    public IdentityGuard(@Value("${internal.token:}") String internalToken) {
        this.expectedInternalToken = internalToken == null || internalToken.isBlank()
                ? null
                : internalToken.getBytes(StandardCharsets.UTF_8);
    }

    @PostConstruct
    void warnIfInternalTokenMissing() {
        if (expectedInternalToken == null) {
            // Sır değeri ASLA loglanmaz; yalnızca eksik olduğu bildirilir.
            log.warn("internal.token tanimli degil — servis ici (Feign) cagrilar reddedilecek (fail-closed). "
                    + "INTERNAL_TOKEN ortam degiskenini verin.");
        }
    }

    /** Servisler arası (Feign) çağrı: YALNIZCA geçerli X-Internal-Token varsa doğrudur. */
    public boolean isInternalCall() {
        if (expectedInternalToken == null) {
            return false;
        }
        String presented = header(HEADER_INTERNAL_TOKEN);
        if (presented == null || presented.isBlank()) {
            return false;
        }
        // Sabit zamanlı karşılaştırma: token içeriği timing ile sızmaz.
        return MessageDigest.isEqual(expectedInternalToken, presented.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isLibrarian() {
        return ROLE_LIBRARIAN.equalsIgnoreCase(header(HEADER_ROLE));
    }

    /** Başlıktaki öğrenci id'si; yoksa ya da parse edilemiyorsa null. */
    public Long currentStudentId() {
        String value = header(HEADER_STUDENT_ID);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** İç çağrı veya LIBRARIAN ise geç; aksi halde kaynak sahibi olunmalı (fail-closed). */
    public void requireSelf(Long id) {
        if (isInternalCall() || isLibrarian()) {
            return;
        }
        Long current = currentStudentId();
        if (current == null || !current.equals(id)) {
            throw new ForbiddenException("Access denied: not your resource");
        }
    }

    /** requireSelf ile aynı; iç çağrı kontrolünü açıkça vurgular. */
    public void requireSelfOrInternal(Long id) {
        requireSelf(id);
    }

    /** Yalnız LIBRARIAN (ve iç çağrı) erişebilir; aksi halde fail-closed. */
    public void requireLibrarian() {
        if (isInternalCall() || isLibrarian()) {
            return;
        }
        throw new ForbiddenException("Access denied: librarian role required");
    }

    private String header(String name) {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader(name);
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
