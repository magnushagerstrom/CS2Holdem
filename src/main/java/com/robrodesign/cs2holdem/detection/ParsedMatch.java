package com.robrodesign.cs2holdem.detection;

import java.time.Instant;
import java.util.List;

/** Everything the demo parser extracts from one match. */
public record ParsedMatch(
        long matchId,
        String mapName,
        Instant playedAt,
        int durationSeconds,
        int team1Score,
        int team2Score,
        List<ParsedRound> rounds,
        List<ParsedKill> kills
) {
    public ParsedMatch {
        rounds = List.copyOf(rounds);
        kills = List.copyOf(kills);
    }
}
