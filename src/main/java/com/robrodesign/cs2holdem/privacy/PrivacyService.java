package com.robrodesign.cs2holdem.privacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deletes a player's data on request, and remembers that they do not want to be stored again.
 *
 * Plain SQL instead of JPA entities: deleting is one statement per table, and this way nothing
 * has to be loaded into memory first. The order matters because of the foreign keys: rows that
 * point at the player go before the player row itself.
 *
 * Shared data stays: a match the player was in still exists, with everyone else's cards and
 * statistics. Only what is about this player is removed.
 */
@Service
public class PrivacyService {

    private static final Logger log = LoggerFactory.getLogger(PrivacyService.class);

    // Season hand items go with their hand (ON DELETE CASCADE), the same for kills pointing at cards
    // ":ids" is a list, so the same statements delete one player or a whole batch.
    private static final List<String> DELETE_STATEMENTS = List.of(
            "DELETE FROM season_hands WHERE steam_id64 IN (:ids)",
            "DELETE FROM kills WHERE steam_id64 IN (:ids)",
            "DELETE FROM cards WHERE steam_id64 IN (:ids)",
            "DELETE FROM match_player_stats WHERE steam_id64 IN (:ids)",
            "DELETE FROM match_history WHERE steam_id64 IN (:ids)",
            "DELETE FROM app_users WHERE steam_id64 IN (:ids)",
            "DELETE FROM players WHERE steam_id64 IN (:ids)");

    /*
     * Unregistered players whose last match is older than the cutoff. "Last match" is when it was
     * played, or when we found it if the demo did not say. A player row without any statistics
     * (seen only in a kill) counts from when the row was created.
     */
    private static final String FIND_EXPIRED = """
            SELECT p.steam_id64
            FROM players p
            WHERE p.created_at < :cutoff
              AND NOT EXISTS (SELECT 1 FROM app_users u WHERE u.steam_id64 = p.steam_id64)
              AND NOT EXISTS (
                  SELECT 1
                  FROM match_player_stats s
                  JOIN matches m ON m.match_id = s.match_id
                  WHERE s.steam_id64 = p.steam_id64
                    AND COALESCE(m.played_at, m.created_at) >= :cutoff)
            LIMIT :limit
            """;

    private final JdbcClient jdbc;

    public PrivacyService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Removes everything about the player and adds them to the opt-out list.
     * One transaction: either all of it is deleted, or nothing is.
     */
    @Transactional
    public void deleteEverything(long steamId64) {
        int rows = deleteData(List.of(steamId64));
        jdbc.sql("INSERT INTO privacy_opt_outs (steam_id64) VALUES (:id) ON CONFLICT DO NOTHING")
                .param("id", steamId64)
                .update();
        // Only the count is logged; what was deleted is nobody's business now
        log.info("Deleted all data of a player on request ({} rows)", rows);
    }

    /**
     * Deletes unregistered players who have not been seen in a match since the cutoff.
     * Unlike deleteEverything they are NOT put on the opt-out list: they never asked for anything,
     * so if they play with a registered friend again they are stored again and the clock restarts.
     *
     * @param limit at most this many players per call, so one run never holds a huge transaction
     * @return how many players were deleted
     */
    @Transactional
    public int deleteExpiredUnregistered(Instant cutoff, int limit) {
        List<Long> expired = jdbc.sql(FIND_EXPIRED)
                .param("cutoff", Timestamp.from(cutoff))
                .param("limit", limit)
                .query(Long.class)
                .list();
        if (expired.isEmpty()) {
            return 0;
        }
        deleteData(expired);
        return expired.size();
    }

    private int deleteData(List<Long> steamIds) {
        int rows = 0;
        for (String statement : DELETE_STATEMENTS) {
            rows += jdbc.sql(statement).param("ids", steamIds).update();
        }
        return rows;
    }

    /** The players among these who asked not to be stored. */
    @Transactional(readOnly = true)
    public Set<Long> optedOut(Collection<Long> steamIds) {
        if (steamIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.sql("SELECT steam_id64 FROM privacy_opt_outs WHERE steam_id64 IN (:ids)")
                .param("ids", steamIds)
                .query(Long.class)
                .list());
    }

    /** Registering is asking to be stored, so it ends an earlier opt-out. */
    @Transactional
    public void optIn(long steamId64) {
        jdbc.sql("DELETE FROM privacy_opt_outs WHERE steam_id64 = :id").param("id", steamId64).update();
    }
}
