package com.robrodesign.cs2holdem.api;

public record LeaderboardEntryView(int rank, String steamId64, String displayName, String avatarUrl, String type,
                                   String typeName, int score, int totalDamage, boolean custom) {
}
