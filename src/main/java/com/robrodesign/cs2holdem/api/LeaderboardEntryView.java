package com.robrodesign.cs2holdem.api;

public record LeaderboardEntryView(int rank, String steamId64, String displayName, String type,
                                   String typeName, int score, int totalDamage, boolean custom) {
}
