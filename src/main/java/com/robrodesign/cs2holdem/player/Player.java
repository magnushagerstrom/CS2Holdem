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

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    /** When we last asked Steam for the name and avatar. null = never. */
    @Column(name = "profile_checked_at")
    private Instant profileCheckedAt;

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

    /**
     * Stores what Steam says about the player now. Steam names can be up to 32 characters, but
     * we cut at the column size anyway, so an unexpected long name can never break the save.
     */
    public void updateProfile(String name, String avatarUrl, Instant checkedAt) {
        if (name != null && !name.isBlank()) {
            this.displayName = name.length() > 64 ? name.substring(0, 64) : name;
        }
        if (avatarUrl != null && avatarUrl.length() <= 255) {
            this.avatarUrl = avatarUrl;
        }
        this.profileCheckedAt = checkedAt;
    }
}
