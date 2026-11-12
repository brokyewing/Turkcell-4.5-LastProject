package com.turkcell.libraryapp.reservationservice.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Feign çağrılarına servis içi paylaşılan sırrı (X-Internal-Token) ekler.
 *
 * <p>Gateway bu başlığı dışarıdan gelen isteklerden siler; burada her giden istekte yeniden
 * yazılır. Token boşsa başlık EKLENMEZ (karşı taraf fail-closed davranıp 403 döner).
 */
public class InternalAuthFeignConfig {

    private final String internalToken;

    public InternalAuthFeignConfig(@Value("${internal.token:}") String internalToken) {
        this.internalToken = internalToken;
    }

    @Bean
    public RequestInterceptor internalAuthRequestInterceptor() {
        return template -> {
            if (internalToken != null && !internalToken.isBlank()) {
                template.header("X-Internal-Token", internalToken);
            }
        };
    }
}
