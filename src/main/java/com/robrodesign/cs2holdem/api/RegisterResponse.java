package com.robrodesign.cs2holdem.api;

// Det vi skickar tillbaka. Notera vad som INTE finns med: auth coden. Den lämnar aldrig servern.
public record RegisterResponse(String steamId64, int matchesFound) {
}
