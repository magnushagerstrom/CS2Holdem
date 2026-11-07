package com.robrodesign.cs2holdem.pipeline;

import com.robrodesign.cs2holdem.match.Match;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/** The database queries the demo pipeline needs. A second repository for Match, kept next to its only user. */
public interface PipelineMatchRepository extends Repository<Match, Long> {

    /**
     * Finds matches in a status that nobody is working on, and locks their rows.
     *
     * FOR UPDATE SKIP LOCKED is what lets several workers run at once: each one locks the rows
     * it takes, and the others skip locked rows instead of waiting for them, so no match is
     * handed out twice. Must run inside a transaction (the lock lasts until it commits).
     */
    @Query(value = """
            SELECT * FROM matches
            WHERE status IN (:statuses)
              AND (claimed_at IS NULL OR claimed_at < :abandonedBefore)
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Match> lockClaimable(@Param("statuses") List<String> statuses,
                              @Param("abandonedBefore") Instant abandonedBefore,
                              @Param("limit") int limit);
}
