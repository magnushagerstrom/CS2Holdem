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

    public void markDownloaded() {
        this.status = MatchStatus.DOWNLOADED;
    }

    public void markParsed(String mapName, Instant playedAt, int durationSeconds, int team1Score, int team2Score) {
        this.mapName = mapName;
        this.playedAt = playedAt;
        this.durationSeconds = durationSeconds;
        this.team1Score = team1Score;
        this.team2Score = team2Score;
        this.status = MatchStatus.PARSED;
    }

    public void markFailed() {
        this.status = MatchStatus.FAILED;
    }
}
