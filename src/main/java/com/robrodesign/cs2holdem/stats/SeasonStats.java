package com.robrodesign.cs2holdem.stats;

import com.robrodesign.cs2holdem.match.StatsTotals;

/**
 * A player's statistics for a season, ready to show: the sums from the database turned into
 * the averages and percentages players know from other stat sites.
 *
 * A value is null when it cannot be calculated (no rounds, no shots ...). Showing "-" is more
 * honest than showing 0 %, which would look like a terrible result.
 */
public record SeasonStats(
        long matches, long wins, long losses, long draws, Double winRate,
        long kills, long deaths, long assists, long flashAssists, Double killDeathRatio,
        Double headshotPercent, Double averageDamagePerRound, Double utilityDamagePerRound,
        Double accuracyPercent, Double timeToKillSeconds,
        long openingKills, long openingDeaths, Double openingSuccessPercent,
        long mvps, long roundsPlayed
) {
    static final double TICKS_PER_SECOND = 64.0;

    public static SeasonStats from(StatsTotals t) {
        long draws = t.matches() - t.wins() - t.losses();
        return new SeasonStats(
                t.matches(), t.wins(), t.losses(), draws, percent(t.wins(), t.matches()),
                t.kills(), t.deaths(), t.assists(), t.flashAssists(),
                // With no deaths the ratio is the number of kills, as on most stat sites
                t.kills() == 0 && t.deaths() == 0 ? null : (double) t.kills() / Math.max(1, t.deaths()),
                percent(t.headshotKills(), t.kills()),
                ratio(t.damage(), t.roundsPlayed()),
                ratio(t.utilityDamage(), t.roundsPlayed()),
                percent(t.shotsHit(), t.shotsFired()),
                t.ttkKills() == 0 ? null : t.ttkTicks() / TICKS_PER_SECOND / t.ttkKills(),
                t.openingKills(), t.openingDeaths(),
                percent(t.openingKills(), t.openingKills() + t.openingDeaths()),
                t.mvps(), t.roundsPlayed());
    }

    static Double percent(long part, long whole) {
        return whole == 0 ? null : 100.0 * part / whole;
    }

    static Double ratio(long amount, long per) {
        return per == 0 ? null : (double) amount / per;
    }
}
