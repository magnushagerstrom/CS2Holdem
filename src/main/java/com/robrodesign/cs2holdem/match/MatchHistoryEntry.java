package com.robrodesign.cs2holdem.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** "Match X finns i användare Y:s historik." */
@Entity
@Table(name = "match_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Vanliga Long-fält i stället för @ManyToOne: vi behöver bara ID:na, inte hela objekten.
    @Column(name = "steam_id64", nullable = false)
    private Long steamId64;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @CreationTimestamp
    @Column(name = "discovered_at", nullable = false, updatable = false)
    private Instant discoveredAt;

    public MatchHistoryEntry(Long steamId64, Long matchId) {
        this.steamId64 = steamId64;
        this.matchId = matchId;
    }
}
