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
        List<ParsedKill> kills,
        List<ParsedPlayer> players
) {
    public ParsedMatch {
        rounds = List.copyOf(rounds);
        kills = List.copyOf(kills);
        // An older worker sends no player totals; treat that as "none" rather than crashing
        players = players == null ? List.of() : List.copyOf(players);
    }

    /** A copy with another play time, for demos that do not say when they were played. */
    public ParsedMatch withPlayedAt(Instant time) {
        return new ParsedMatch(matchId, mapName, time, durationSeconds, team1Score, team2Score, rounds, kills, players);
    }
}
