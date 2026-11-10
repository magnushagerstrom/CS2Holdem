package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.stats.PlayerProfile;
import com.robrodesign.cs2holdem.stats.SeasonStats;

import java.util.List;

/** A player's profile page as the API shows it. The SteamID is a string, as everywhere in the API. */
public record PlayerStatsView(
        String steamId64, String displayName, String avatarUrl,
        SeasonView season, SeasonStats stats,
        List<PlayerProfile.Count> cards, long clutches, List<PlayerProfile.Count> weapons
) {
    public static PlayerStatsView from(PlayerProfile p) {
        return new PlayerStatsView(String.valueOf(p.steamId64()), p.displayName(), p.avatarUrl(),
                SeasonView.from(p.season()), p.stats(), p.cards(), p.clutches(), p.weapons());
    }
}
