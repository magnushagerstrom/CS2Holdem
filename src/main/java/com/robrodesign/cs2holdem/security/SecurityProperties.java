package com.robrodesign.cs2holdem.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param encryptionKey   base64 AES key for auth codes (APP_ENCRYPTION_KEY)
 * @param workerToken     shared secret the demo worker sends in X-Worker-Token (WORKER_TOKEN)
 * @param publicBaseUrl   where the app is reached from the outside, used for Steam's login redirect
 * @param afterLoginUrl   where the browser is sent after a successful login
 * @param devLoginEnabled allows /auth/dev-login?steamId64=... without Steam. NEVER in production.
 */
@Validated
@ConfigurationProperties(prefix = "cs2holdem.security")
public record SecurityProperties(
        @NotBlank String encryptionKey,
        @NotBlank String workerToken,
        @NotBlank String publicBaseUrl,
        @NotBlank String afterLoginUrl,
        boolean devLoginEnabled
) {
    @Override
    public String toString() {
        return "SecurityProperties[encryptionKey=***, workerToken=***, publicBaseUrl=" + publicBaseUrl
                + ", afterLoginUrl=" + afterLoginUrl + ", devLoginEnabled=" + devLoginEnabled + "]";
    }
}
