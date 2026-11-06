package com.robrodesign.cs2holdem.match;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    // Optional = "kanske finns, kanske inte". Tvingar den som anropar att hantera båda fallen.
    Optional<Match> findByShareCode(String shareCode);

    boolean existsByShareCode(String shareCode);

    List<Match> findTop50ByStatusOrderByCreatedAtAsc(MatchStatus status);

    // JPQL: ser ut som SQL men pratar om Java-klasser och fält (Match, matchId), inte tabeller och kolumner.
    @Query("""
            select m from Match m
            where m.matchId in (
                select h.matchId from MatchHistoryEntry h where h.steamId64 = :steamId64
            )
            order by m.createdAt desc
            """)
    List<Match> findMatchesInHistoryOf(@Param("steamId64") Long steamId64);
}
