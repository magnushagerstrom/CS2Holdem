package com.robrodesign.cs2holdem.match;

/**
 * A player's summed statistics over many matches, straight from the database.
 * Long because SQL's SUM and COUNT return 64-bit numbers; COALESCE in the query makes them never null.
 */
public record StatsTotals(
        Long matches, Long wins, Long losses,
        Long kills, Long deaths, Long assists, Long flashAssists, Long headshotKills,
        Long damage, Long utilityDamage, Long roundsPlayed, Long mvps,
        Long shotsFired, Long shotsHit, Long openingKills, Long openingDeaths,
        Long ttkTicks, Long ttkKills
) {
}
