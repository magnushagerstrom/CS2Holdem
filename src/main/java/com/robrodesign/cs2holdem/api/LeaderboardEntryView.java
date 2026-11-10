package com.robrodesign.cs2holdem.api;

/** registered: only registered players have a profile page the leaderboard can link to. */
public record LeaderboardEntryView(int rank, String steamId64, String displayName, String avatarUrl, String type,
                                   String typeName, int score, int totalDamage, boolean custom,
                                   boolean registered) {
}
