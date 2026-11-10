package com.robrodesign.cs2holdem.player;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    /**
     * Players whose Steam name we have never fetched, or fetched before `checkedBefore`.
     * Never-checked players come first, so a new player's name shows up within a minute.
     */
    @Query("""
            SELECT p FROM Player p
            WHERE p.profileCheckedAt IS NULL OR p.profileCheckedAt < :checkedBefore
            ORDER BY p.profileCheckedAt ASC NULLS FIRST
            """)
    List<Player> findProfilesToRefresh(@Param("checkedBefore") Instant checkedBefore, Limit limit);
}
