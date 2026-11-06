package com.robrodesign.cs2holdem.account;

import com.robrodesign.cs2holdem.player.Player;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.Instant;

/**
 * En spelare som har registrerat sig i appen och gett oss sin CS2 auth code,
 * så att vi kan hämta deras matchhistorik från Steam.
 */
@Entity
@Table(name = "app_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser {

    @Id
    @Column(name = "steam_id64")
    private Long steamId64;

    // @MapsId: primärnyckeln hämtas från spelaren. En app-användare ÄR en spelare.
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "steam_id64")
    private Player player;

    // Krypteras i steg 2. Får aldrig loggas eller skickas ut via API:t.
    @Column(name = "auth_code", length = 512, nullable = false)
    private String authCode;

    @Column(name = "last_known_share_code", length = 40, nullable = false)
    private String lastKnownShareCode;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "next_sync_at", nullable = false)
    private Instant nextSyncAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AppUser(Player player, String authCode, String lastKnownShareCode) {
        this.player = player;
        this.authCode = authCode;
        this.lastKnownShareCode = lastKnownShareCode;
        this.nextSyncAt = Instant.now(); // ny användare: synka direkt
    }

    /** Anropas när synken hittat en nyare match. Flyttar fram bokmärket. */
    public void recordNewShareCode(String shareCode) {
        this.lastKnownShareCode = shareCode;
    }

    /** Anropas efter varje synk-försök. Bestämmer när det är dags nästa gång. */
    public void markSynced(Instant now, Duration untilNextSync) {
        this.lastSyncedAt = now;
        this.nextSyncAt = now.plus(untilNextSync);
    }
}
