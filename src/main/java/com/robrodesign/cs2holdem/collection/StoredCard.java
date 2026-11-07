package com.robrodesign.cs2holdem.collection;

import com.robrodesign.cs2holdem.hand.EventType;
import com.robrodesign.cs2holdem.hand.Stamp;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * A card as it is stored in the database. The hand engine never sees this class;
 * {@link CardMapper} turns it into a {@code hand.Event}.
 */
@Entity
@Table(name = "cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "steam_id64", nullable = false)
    private Long steamId64;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private EventType type;

    @Convert(converter = StampSetConverter.class)
    @Column(name = "stamps", length = 200, nullable = false)
    private Set<Stamp> stamps = EnumSet.noneOf(Stamp.class);

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public StoredCard(long steamId64, long matchId, int roundNumber, EventType type, Set<Stamp> stamps) {
        this.steamId64 = steamId64;
        this.matchId = matchId;
        this.roundNumber = roundNumber;
        this.type = type;
        this.stamps = stamps.isEmpty() ? EnumSet.noneOf(Stamp.class) : EnumSet.copyOf(stamps);
    }
}
