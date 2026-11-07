package com.robrodesign.cs2holdem.collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface StoredCardRepository extends JpaRepository<StoredCard, Long> {

    // Alla kort spelaren fått i matcher som spelades mellan from (inklusive) och to (exklusive).
    @Query("""
            select c from StoredCard c
            where c.steamId64 = :steamId64
              and c.matchId in (
                  select m.matchId from Match m where m.playedAt >= :from and m.playedAt < :to
              )
            order by c.matchId, c.roundNumber
            """)
    List<StoredCard> findForPlayerBetween(@Param("steamId64") long steamId64,
                                          @Param("from") Instant from,
                                          @Param("to") Instant to);

    // Tar bort allt från en match, så att en omparsad demo inte ger dubbletter.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from StoredCard c where c.matchId = :matchId")
    void deleteForMatch(@Param("matchId") long matchId);
}
