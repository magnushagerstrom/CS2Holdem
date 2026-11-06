package com.robrodesign.cs2holdem.steam;

/** Steam svarade inte, eller svarade med ett serverfel. Värt att försöka igen senare. */
public class SteamUnavailableException extends RuntimeException {
    public SteamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
