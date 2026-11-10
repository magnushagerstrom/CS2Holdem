package com.robrodesign.cs2holdem.detection;

/**
 * Everything worth showing about one player in one match, as raw sums.
 *
 * Only sums are stored, never averages or percentages: an average of averages is wrong when
 * matches have different lengths, but sums can always be added up and divided afterwards.
 *
 * @param team          1 = the team that ended on CT (team1Score), 2 = the team that ended on T
 * @param ttkTicks      the summed time to kill of the kills in {@code ttkKills}, in game ticks
 * @param ttkKills      gun kills where we know when the first hit landed
 */
public record PlayerMatchStats(
        long steamId,
        String name,
        int team,
        MatchResult result,
        int kills,
        int deaths,
        int assists,
        int flashAssists,
        int headshotKills,
        int damage,
        int utilityDamage,
        int roundsPlayed,
        int mvps,
        int shotsFired,
        int shotsHit,
        int openingKills,
        int openingDeaths,
        long ttkTicks,
        int ttkKills
) {
    public enum MatchResult { WIN, LOSS, DRAW }
}
