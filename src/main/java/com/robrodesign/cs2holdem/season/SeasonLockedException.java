package com.robrodesign.cs2holdem.season;

public class SeasonLockedException extends RuntimeException {
    public SeasonLockedException(String seasonName) {
        super("Season " + seasonName + " has ended and its hands are locked");
    }
}
