package com.robrodesign.cs2holdem.hand;

public enum HandType {
    HIGH_CARD("High card", 10),
    PAIR("Pair", 20),
    TWO_PAIR("Two pair", 35),
    THREE_OF_A_KIND("Three of a kind", 50),
    STRAIGHT("Straight", 70),
    FLUSH("Flush", 90),
    FULL_HOUSE("Full house", 110),
    FOUR_OF_A_KIND("Four of a kind", 140),
    STRAIGHT_FLUSH("Straight flush", 170),
    FIVE_OF_A_KIND("Five of a kind", 200),
    ROYAL_FLUSH("Royal flush", 250);

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
