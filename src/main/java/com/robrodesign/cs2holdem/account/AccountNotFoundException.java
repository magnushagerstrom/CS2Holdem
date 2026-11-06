package com.robrodesign.cs2holdem.account;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(long steamId64) {
        super("No account found for SteamID " + steamId64);
    }
}
