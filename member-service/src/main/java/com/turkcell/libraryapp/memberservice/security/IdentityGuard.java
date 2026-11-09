package com.turkcell.libraryapp.memberservice.security;

import com.turkcell.libraryapp.memberservice.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Gateway'in token'dan türetip eklediği X-Role / X-Student-Id başlıklarına dayanan sahiplik kontrolü.
 *
 * <p>Kural: LIBRARIAN her şeyi yapar; başlık YOKSA istek servisler arası (Feign) sayılır ve serbesttir;
 * X-Student-Id varsa ve rol STUDENT ise kaynağın sahibi olmak zorunludur.
 *
 * <p>İstek, RequestContextHolder üzerinden okunur; böylece controller'lar request-scoped proxy'ye
 * bağlı kalmaz (standalone MockMvc testleri de çalışır).
 */
@Component
public class IdentityGuard {

    public static final String HEADER_ROLE = "X-Role";
    public static final String HEADER_STUDENT_ID = "X-Student-Id";
    private static final String ROLE_LIBRARIAN = "LIBRARIAN";

    /** Servisler arası (Feign) çağrı: gateway X-Role eklemez. */
    public boolean isInternalCall() {
        String role = header(HEADER_ROLE);
        return role == null || role.isBlank();
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

    /** İç çağrı veya LIBRARIAN ise geç; aksi halde kaynak sahibi olunmalı. */
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

    /** Yalnız LIBRARIAN (ve iç çağrı) erişebilir. */
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
