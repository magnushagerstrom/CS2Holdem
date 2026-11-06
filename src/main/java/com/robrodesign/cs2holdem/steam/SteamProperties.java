package com.robrodesign.cs2holdem.steam;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Inställningarna under "steam:" i application.yaml, samlade i ett typsäkert objekt.
 * @Validated gör att appen vägrar starta om api-key saknas, i stället för att krascha senare.
 */
@Validated
@ConfigurationProperties(prefix = "steam")
public record SteamProperties(
        @NotBlank String apiKey,
        @NotBlank String baseUrl
) {
    // Skriv aldrig ut nyckeln, inte ens av misstag i en logg.
    @Override
    public String toString() {
        return "SteamProperties[apiKey=***, baseUrl=" + baseUrl + "]";
    }
}
