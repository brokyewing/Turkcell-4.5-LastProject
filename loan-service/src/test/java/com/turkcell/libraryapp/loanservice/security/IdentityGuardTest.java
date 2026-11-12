package com.turkcell.libraryapp.loanservice.security;

import com.turkcell.libraryapp.loanservice.exception.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * H1: IdentityGuard fail-closed. Başlıksız istek artık "servis içi" sayılmaz;
 * yalnız GEÇERLİ X-Internal-Token taşıyan çağrılar serbesttir.
 */
class IdentityGuardTest {

    private static final String TOKEN = "test-internal-token";

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    /** İstek bağlamına verilen başlıkları kurar. */
    private void givenRequest(String... nameValuePairs) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        for (int i = 0; i + 1 < nameValuePairs.length; i += 2) {
            request.addHeader(nameValuePairs[i], nameValuePairs[i + 1]);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void basliksizIstekReddedilir() {
        // ESKİDEN GEÇİYORDU (X-Role yok → iç çağrı). Asıl regresyon testi.
        givenRequest();
        IdentityGuard guard = new IdentityGuard(TOKEN);

        assertFalse(guard.isInternalCall());
        assertThrows(ForbiddenException.class, () -> guard.requireSelf(1L));
        assertThrows(ForbiddenException.class, () -> guard.requireLibrarian());
    }

    @Test
    void dogruInternalTokenIleGecer() {
        givenRequest(IdentityGuard.HEADER_INTERNAL_TOKEN, TOKEN);
        IdentityGuard guard = new IdentityGuard(TOKEN);

        assertTrue(guard.isInternalCall());
        assertDoesNotThrow(() -> guard.requireSelf(1L));
        assertDoesNotThrow(() -> guard.requireSelfOrInternal(99L));
        assertDoesNotThrow(() -> guard.requireLibrarian());
    }

    @Test
    void yanlisInternalTokenReddedilir() {
        givenRequest(IdentityGuard.HEADER_INTERNAL_TOKEN, "yanlis-token");
        IdentityGuard guard = new IdentityGuard(TOKEN);

        assertFalse(guard.isInternalCall());
        assertThrows(ForbiddenException.class, () -> guard.requireSelf(1L));
        assertThrows(ForbiddenException.class, () -> guard.requireLibrarian());
    }

    @Test
    void rolVarkenYanlisTokenYineReddedilir() {
        givenRequest(IdentityGuard.HEADER_ROLE, "LIBRARIAN",
                IdentityGuard.HEADER_INTERNAL_TOKEN, "yanlis-token");
        IdentityGuard guard = new IdentityGuard(TOKEN);

        // LIBRARIAN rolü kendi başına geçerli; internal token yanlış olsa da rol kontrolü galip.
        assertFalse(guard.isInternalCall());
        assertDoesNotThrow(() -> guard.requireLibrarian());
    }

    @Test
    void tokenTanimliDegilseHicbirIcCagriKabulEdilmez() {
        // internal.token boş → fail-closed: X-Internal-Token gönderilse bile geçmez.
        givenRequest(IdentityGuard.HEADER_INTERNAL_TOKEN, "");
        IdentityGuard guard = new IdentityGuard("");

        assertFalse(guard.isInternalCall());
        assertThrows(ForbiddenException.class, () -> guard.requireSelf(1L));
    }

    @Test
    void studentYalnizKendiKaynaginaErisir() {
        givenRequest(IdentityGuard.HEADER_ROLE, "STUDENT",
                IdentityGuard.HEADER_STUDENT_ID, "1");
        IdentityGuard guard = new IdentityGuard(TOKEN);

        assertThrows(ForbiddenException.class, () -> guard.requireSelf(2L));
        assertDoesNotThrow(() -> guard.requireSelf(1L));
        assertThrows(ForbiddenException.class, () -> guard.requireLibrarian());
    }

    @Test
    void librarianHerKaynagaErisir() {
        givenRequest(IdentityGuard.HEADER_ROLE, "LIBRARIAN",
                IdentityGuard.HEADER_STUDENT_ID, "1");
        IdentityGuard guard = new IdentityGuard(TOKEN);

        assertDoesNotThrow(() -> guard.requireSelf(2L));
        assertDoesNotThrow(() -> guard.requireLibrarian());
    }
}
