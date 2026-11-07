package com.robrodesign.cs2holdem.api;

public class NotYourAccountException extends RuntimeException {
    public NotYourAccountException() {
        super("You can only change your own account and hands");
    }
}
