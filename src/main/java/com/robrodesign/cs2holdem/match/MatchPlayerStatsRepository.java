package com.robrodesign.cs2holdem.match;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchPlayerStatsRepository extends JpaRepository<MatchPlayerStats, Long> {

    // "player.steamId64" i metodnamnet: Spring Data följer relationen till Player
    List<MatchPlayerStats> findByPlayerSteamId64(Long steamId64);
}
