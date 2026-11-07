package com.robrodesign.cs2holdem.collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface StoredKillRepository extends JpaRepository<StoredKill, Long> {

    @Query("""
            select k from StoredKill k
            where k.steamId64 = :steamId64
              and k.matchId in (
                  select m.matchId from Match m where m.playedAt >= :from and m.playedAt < :to
              )
            order by k.matchId, k.roundNumber, k.tick
            """)
    List<StoredKill> findForPlayerBetween(@Param("steamId64") long steamId64,
                                          @Param("from") Instant from,
                                          @Param("to") Instant to);

    // Tar bort allt från en match, så att en omparsad demo inte ger dubbletter.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from StoredKill k where k.matchId = :matchId")
    void deleteForMatch(@Param("matchId") long matchId);
}
