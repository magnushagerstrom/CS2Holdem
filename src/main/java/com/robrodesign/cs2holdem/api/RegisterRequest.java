package com.robrodesign.cs2holdem.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Det klienten skickar in för att registrera sig.
 *
 * steamId64 är en STRÄNG i JSON, inte ett tal. JavaScript kan bara räkna exakt med heltal upp till
 * ungefär 9 biljarder (2^53), och ett SteamID är större än så. Ett webbgränssnitt skulle
 * annars tyst avrunda ID:t och få fel spelare.
 */
public record RegisterRequest(
        @NotBlank
        @Pattern(regexp = "^7656119\\d{10}$", message = "must be a 17-digit SteamID64")
        String steamId64,

        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9]{4}-[A-Za-z0-9]{5}-[A-Za-z0-9]{4}$", message = "must look like XXXX-XXXXX-XXXX")
        String authCode,

        @NotBlank
        @Pattern(regexp = "^CSGO(-[A-Za-z0-9]{5}){5}$", message = "must look like CSGO-xxxxx-xxxxx-xxxxx-xxxxx-xxxxx")
        String knownShareCode
) {
    // Records skriver annars ut alla fält i toString(), och då kunde auth coden hamna i en logg.
    @Override
    public String toString() {
        return "RegisterRequest[steamId64=" + steamId64 + ", authCode=***, knownShareCode=" + knownShareCode + "]";
    }
}
