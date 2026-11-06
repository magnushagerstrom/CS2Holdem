package com.robrodesign.cs2holdem.steam;

public class InvalidShareCodeException extends RuntimeException {
    public InvalidShareCodeException(String message) {
        super(message);
    }
}
