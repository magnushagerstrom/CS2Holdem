package com.robrodesign.cs2holdem.stats;

import java.util.Map;

/**
 * Steam's lifetime CS2 numbers, all game modes together. available = false for private profiles.
 * Ratios are null when Steam does not report what they are made of.
 */
public record LifetimeStatsView(
        boolean available, Long kills, Long deaths, Double killDeathRatio, Double headshotPercent,
        Double accuracyPercent, Long matchesWon, Long mvps, Long hoursPlayed
) {
    public static LifetimeStatsView unavailable() {
        return new LifetimeStatsView(false, null, null, null, null, null, null, null, null);
    }

    public static LifetimeStatsView from(Map<String, Long> s) {
        Long kills = s.get("total_kills");
        Long deaths = s.get("total_deaths");
        Long seconds = s.get("total_time_played");
        return new LifetimeStatsView(true, kills, deaths,
                kills == null || deaths == null ? null : (double) kills / Math.max(1, deaths),
                part(s.get("total_kills_headshot"), kills),
                part(s.get("total_shots_hit"), s.get("total_shots_fired")),
                s.get("total_matches_won"), s.get("total_mvps"),
                seconds == null ? null : seconds / 3600);
    }

    private static Double part(Long part, Long whole) {
        return part == null || whole == null || whole == 0 ? null : 100.0 * part / whole;
    }
}
