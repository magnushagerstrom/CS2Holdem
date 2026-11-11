package com.robrodesign.cs2holdem.hand;

public enum HandType {
    HIGH_CARD("High card", 100),
    PAIR("Pair", 200),
    TWO_PAIR("Two pair", 350),
    THREE_OF_A_KIND("Three of a kind", 500),
    STRAIGHT("Straight", 700),
    FLUSH("Flush", 900),
    FULL_HOUSE("Full house", 1100),
    FOUR_OF_A_KIND("Four of a kind", 1400),
    STRAIGHT_FLUSH("Straight flush", 1700),
    FIVE_OF_A_KIND("Five of a kind", 2000),
    ROYAL_FLUSH("Royal flush", 2500);

    private final String displayName;

    private final int basePoints;

    HandType(String displayName, int basePoints) {
        this.displayName = displayName;
        this.basePoints = basePoints;
    }

    public String displayName() {
        return displayName;
    }

    public int basePoints() {
        return basePoints;
    }
}
