package com.robrodesign.cs2holdem.hand;

public enum EventType {
    SPECIAL_KILL("Special kill", 1),
    DOUBLE_KILL("2x kill", 2),
    TRIPLE_KILL("3x kill", 3),
    QUAD_KILL("4x kill", 4),
    ACE("Ace", 5);

    private final String displayName;
    private final int killCount;

    EventType(String displayName, int killCount) {
        this.displayName = displayName;
        this.killCount = killCount;
    }

    public String displayName() {
        return displayName;
    }

    public int killCount() {
        return killCount;
    }
}
