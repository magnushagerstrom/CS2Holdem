package com.robrodesign.cs2holdem.season;

/** The cards a player picked do not form a valid hand, or are not theirs to pick. */
public class InvalidHandException extends RuntimeException {
    public InvalidHandException(String message) {
        super(message);
    }
}
