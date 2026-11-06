package com.robrodesign.cs2holdem.account;

public class AccountAlreadyExistsException extends RuntimeException {
    public AccountAlreadyExistsException(long steamId64) {
        super("An account for SteamID " + steamId64 + " already exists");
    }
}
