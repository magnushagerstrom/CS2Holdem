package com.robrodesign.cs2holdem.match;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchHistoryRepository extends JpaRepository<MatchHistoryEntry, Long> {

    boolean existsBySteamId64AndMatchId(Long steamId64, Long matchId);
}
