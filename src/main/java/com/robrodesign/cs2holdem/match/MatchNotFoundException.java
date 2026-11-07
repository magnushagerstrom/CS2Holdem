package com.robrodesign.cs2holdem.match;

public class MatchNotFoundException extends RuntimeException {
    public MatchNotFoundException(long matchId) {
        super("No match with id " + matchId);
    }
}
