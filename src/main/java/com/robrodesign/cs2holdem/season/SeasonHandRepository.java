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

    @EntityGraph(attributePaths = "items")
    List<SeasonHand> findByScoreVersionLessThan(int scoreVersion);

    // Registered players who have no hand in this season. Most of them simply have no cards yet.
    @Query("""
            select u.steamId64 from AppUser u
            where not exists (
                select h from SeasonHand h where h.steamId64 = u.steamId64 and h.seasonId = :seasonId
            )
            """)
    List<Long> findRegisteredWithoutHand(@Param("seasonId") long seasonId);

    @Query("""
            select h from SeasonHand h
            where h.seasonId = :seasonId
            order by h.score desc, h.totalDamage desc, h.updatedAt asc
            """)
    List<SeasonHand> findLeaderboard(@Param("seasonId") long seasonId, Pageable page);
}
