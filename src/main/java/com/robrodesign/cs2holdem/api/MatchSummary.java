package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.match.Match;

import java.time.Instant;

/**
 * En DTO (Data Transfer Object): det API:t visar utåt, frikopplat från databasentiteten.
 * Då kan vi ändra Match-entiteten utan att API:t ändras för de som använder det, och tvärtom.
 */
public record MatchSummary(String matchId, String shareCode, String status, String mapName, Instant discoveredAt) {

    public static MatchSummary from(Match match) {
        return new MatchSummary(
                String.valueOf(match.getMatchId()), // sträng av samma skäl som steamId64
                match.getShareCode(),
                match.getStatus().name(),
                match.getMapName(),
                match.getCreatedAt());
    }
}
