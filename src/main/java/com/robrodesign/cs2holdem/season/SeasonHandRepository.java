package com.robrodesign.cs2holdem.season;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeasonHandRepository extends JpaRepository<SeasonHand, Long> {

    // EntityGraph: hämta items i samma fråga, i stället för en extra fråga per hand
    @EntityGraph(attributePaths = "items")
    Optional<SeasonHand> findBySteamId64AndSeasonId(long steamId64, long seasonId);

    @EntityGraph(attributePaths = "items")
    List<SeasonHand> findBySteamId64OrderBySeasonIdDesc(long steamId64);

    @Query("""
            select h from SeasonHand h
            where h.seasonId = :seasonId
            order by h.score desc, h.totalDamage desc, h.updatedAt asc
            """)
    List<SeasonHand> findLeaderboard(@Param("seasonId") long seasonId, Pageable page);
}
