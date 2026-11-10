package com.robrodesign.cs2holdem.stats;

import com.robrodesign.cs2holdem.match.StatsTotals;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SeasonStatsTest {

    private static StatsTotals totals(long matches, long wins, long losses, long kills, long deaths,
                                      long headshots, long damage, long rounds, long fired, long hit,
                                      long openingKills, long openingDeaths, long ttkTicks, long ttkKills) {
        return new StatsTotals(matches, wins, losses, kills, deaths, 10L, 2L, headshots, damage, 300L,
                rounds, 5L, fired, hit, openingKills, openingDeaths, ttkTicks, ttkKills);
    }

    @Test
    void averagesAndPercentagesAreWorkedOutFromTheSums() {
        SeasonStats stats = SeasonStats.from(totals(4, 3, 1, 80, 40, 40, 8000, 100, 1000, 250, 6, 4, 640, 20));

        assertThat(stats.killDeathRatio()).isEqualTo(2.0);
        assertThat(stats.headshotPercent()).isEqualTo(50.0);
        assertThat(stats.averageDamagePerRound()).isEqualTo(80.0);
        assertThat(stats.accuracyPercent()).isEqualTo(25.0);
        assertThat(stats.timeToKillSeconds()).isEqualTo(0.5); // 640 ticks / 20 kills / 64 per second
        assertThat(stats.openingSuccessPercent()).isEqualTo(60.0);
        assertThat(stats.winRate()).isEqualTo(75.0);
        assertThat(stats.draws()).isEqualTo(0L);
    }

    @Test
    void nothingToDivideByGivesNullInsteadOfZero() {
        SeasonStats stats = SeasonStats.from(totals(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        assertThat(stats.killDeathRatio() == null).isTrue();
        assertThat(stats.headshotPercent() == null).isTrue();
        assertThat(stats.averageDamagePerRound() == null).isTrue();
        assertThat(stats.timeToKillSeconds() == null).isTrue();
        assertThat(stats.winRate() == null).isTrue();
    }

    @Test
    void noDeathsGivesTheKillsAsRatio() {
        SeasonStats stats = SeasonStats.from(totals(1, 1, 0, 7, 0, 0, 700, 13, 0, 0, 0, 0, 0, 0));

        assertThat(stats.killDeathRatio()).isEqualTo(7.0);
    }

    @Test
    void steamLifetimeStatsAreReadByName() {
        LifetimeStatsView view = LifetimeStatsView.from(Map.of(
                "total_kills", 1000L, "total_deaths", 800L, "total_kills_headshot", 450L,
                "total_shots_fired", 20000L, "total_shots_hit", 4000L, "total_time_played", 36000L));

        assertThat(view.available()).isTrue();
        assertThat(view.killDeathRatio()).isEqualTo(1.25);
        assertThat(view.headshotPercent()).isEqualTo(45.0);
        assertThat(view.accuracyPercent()).isEqualTo(20.0);
        assertThat(view.hoursPlayed()).isEqualTo(10L);
    }

    @Test
    void aPrivateProfileHasNoLifetimeStats() {
        assertThat(LifetimeStatsView.unavailable().available()).isFalse();
    }
}
