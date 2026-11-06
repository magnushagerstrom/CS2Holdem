package com.robrodesign.cs2holdem.player;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * En Steam-spelare som har dykt upp i minst en match.
 * Behöver inte vara registrerad i appen (se AppUser för det).
 */
@Entity
@Table(name = "players")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA kräver en tom konstruktor, men ingen annan ska använda den
public class Player {

    @Id
    @Column(name = "steam_id64")
    private Long steamId64;

    @Setter
    @Column(name = "display_name", length = 64)
    private String displayName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Player(Long steamId64, String displayName) {
        this.steamId64 = steamId64;
        this.displayName = displayName;
    }
}
