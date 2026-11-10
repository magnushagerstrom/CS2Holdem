package com.robrodesign.cs2holdem.stats;

import com.robrodesign.cs2holdem.season.Season;

import java.util.List;

/**
 * Everything on a player's profile page, before it is turned into JSON by the api package.
 *
 * @param cards   how many of each card the player collected this season, strongest first
 * @param weapons counted kills per weapon type, most first
 */
public record PlayerProfile(
        long steamId64, String displayName, String avatarUrl,
        Season season, SeasonStats stats,
        List<Count> cards, long clutches, List<Count> weapons
) {
    public record Count(String name, long count) {
    }
}
