package com.robrodesign.cs2holdem.steam;

/** Steam svarade 403: kombinationen SteamID + auth code godkändes inte. */
public class InvalidSteamCredentialsException extends RuntimeException {
    public InvalidSteamCredentialsException() {
        super("Steam rejected the SteamID and auth code combination");
    }
}
