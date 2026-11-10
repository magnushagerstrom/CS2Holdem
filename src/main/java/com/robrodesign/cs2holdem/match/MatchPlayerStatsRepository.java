package com.robrodesign.cs2holdem.match;

import com.robrodesign.cs2holdem.detection.PlayerMatchStats.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface MatchPlayerStatsRepository extends JpaRepository<MatchPlayerStats, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MatchPlayerStats s WHERE s.match.matchId = :matchId")
    void deleteForMatch(@Param("matchId") long matchId);

    /**
     * Adds up a player's matches played in [from, to): one row with every sum.
     * The database does the adding, so even hundreds of matches are one quick query.
     * COALESCE turns "no matches" (SUM of nothing = NULL) into zeros.
     */
    @Query("""
            SELECT new com.robrodesign.cs2holdem.match.StatsTotals(
                COUNT(s),
                COALESCE(SUM(CASE WHEN s.result = :win THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN s.result = :loss THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(s.kills), 0), COALESCE(SUM(s.deaths), 0), COALESCE(SUM(s.assists), 0),
                COALESCE(SUM(s.flashAssists), 0), COALESCE(SUM(s.headshotKills), 0),
                COALESCE(SUM(s.damageTotal), 0), COALESCE(SUM(s.utilityDamage), 0),
                COALESCE(SUM(s.roundsPlayed), 0), COALESCE(SUM(s.mvps), 0),
                COALESCE(SUM(s.shotsFired), 0), COALESCE(SUM(s.shotsHit), 0),
                COALESCE(SUM(s.openingKills), 0), COALESCE(SUM(s.openingDeaths), 0),
                COALESCE(SUM(s.ttkTicks), 0), COALESCE(SUM(s.ttkKills), 0))
            FROM MatchPlayerStats s
            WHERE s.player.steamId64 = :steamId64
              AND s.match.playedAt >= :from AND s.match.playedAt < :to
            """)
    StatsTotals sumStats(@Param("steamId64") long steamId64, @Param("from") Instant from, @Param("to") Instant to,
                         @Param("win") MatchResult win, @Param("loss") MatchResult loss);

    /** The enum values go in as parameters: safer than spelling out a nested enum's name in JPQL. */
    default StatsTotals totals(long steamId64, Instant from, Instant to) {
        return sumStats(steamId64, from, to, MatchResult.WIN, MatchResult.LOSS);
    }
}
