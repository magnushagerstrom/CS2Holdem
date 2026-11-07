package com.robrodesign.cs2holdem.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/** En spelad match. Lagras en gång, oavsett hur många av våra användare som var med. */
@Entity
@Table(name = "matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match {

    public static final int MAX_ATTEMPTS = 5;

    @Id
    @Column(name = "match_id")
    private Long matchId;

    @Column(name = "share_code", length = 40, nullable = false, unique = true)
    private String shareCode;

    @Enumerated(EnumType.STRING) // sparar "PARSED" som text, inte som ett nummer
    @Column(name = "status", length = 20, nullable = false)
    private MatchStatus status;

    @Column(name = "map_name", length = 32)
    private String mapName;

    @Column(name = "played_at")
    private Instant playedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "team1_score")
    private Integer team1Score;

    @Column(name = "team2_score")
    private Integer team2Score;

    // Behövs för att be Game Coordinator om demon. Avkodas ur share coden.
    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "tv_port")
    private Integer tvPort;

    @Column(name = "demo_url", columnDefinition = "TEXT")
    private String demoUrl;

    // Var demon ligger i vår lagring (en S3-nyckel eller en filsökväg)
    @Column(name = "demo_key", columnDefinition = "TEXT")
    private String demoKey;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    // When a worker took this match as a job. Null when nobody is working on it.
    // A claim older than the job's time limit counts as abandoned (the worker probably crashed).
    @Column(name = "claimed_at")
    private Instant claimedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Match(Long matchId, String shareCode) {
        this.matchId = matchId;
        this.shareCode = shareCode;
        this.status = MatchStatus.DISCOVERED;
    }

    public Match(Long matchId, String shareCode, long reservationId, int tvPort) {
        this(matchId, shareCode);
        this.reservationId = reservationId;
        this.tvPort = tvPort;
    }

    /** A worker has taken this match as a job. */
    public void claim(Instant now) {
        this.claimedAt = now;
    }

    public void markDemoLocated(String demoUrl) {
        this.claimedAt = null;
        this.demoUrl = demoUrl;
        this.status = MatchStatus.DEMO_LOCATED;
        this.lastError = null;
    }

    public void markDownloaded(String demoKey) {
        this.claimedAt = null;
        this.demoKey = demoKey;
        this.status = MatchStatus.DOWNLOADED;
        this.lastError = null;
    }

    public void markParsed(String mapName, Instant playedAt, int durationSeconds, int team1Score, int team2Score) {
        this.mapName = mapName;
        this.playedAt = playedAt;
        this.durationSeconds = durationSeconds;
        this.team1Score = team1Score;
        this.team2Score = team2Score;
        this.claimedAt = null;
        this.status = MatchStatus.PARSED;
        this.lastError = null;
        this.demoKey = null; // the stored demo is deleted after parsing
    }

    public void markExpired() {
        this.claimedAt = null;
        this.status = MatchStatus.EXPIRED;
    }

    /**
     * Records a failed attempt. The match stays in its current status so it is retried,
     * until it has failed {@link #MAX_ATTEMPTS} times.
     */
    public void recordFailure(String error) {
        this.claimedAt = null;
        this.attempts++;
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), 2000));
        if (attempts >= MAX_ATTEMPTS) {
            this.status = MatchStatus.FAILED;
        }
    }
}
