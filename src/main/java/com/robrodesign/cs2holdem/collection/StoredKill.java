package com.robrodesign.cs2holdem.collection;

import com.robrodesign.cs2holdem.hand.Stamp;
import com.robrodesign.cs2holdem.hand.WeaponGroup;
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

import java.util.EnumSet;
import java.util.Set;

/** A counted kill as it is stored in the database. See {@link CardMapper}. */
@Entity
@Table(name = "kills")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredKill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "steam_id64", nullable = false)
    private Long steamId64;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "tick", nullable = false)
    private int tick;

    @Enumerated(EnumType.STRING)
    @Column(name = "weapon_group", length = 20, nullable = false)
    private WeaponGroup weaponGroup;

    @Column(name = "difficulty", nullable = false)
    private int difficulty;

    @Column(name = "damage", nullable = false)
    private int damage;

    @Column(name = "map_area", length = 64, nullable = false)
    private String mapArea;

    @Convert(converter = StampSetConverter.class)
    @Column(name = "stamps", length = 200, nullable = false)
    private Set<Stamp> stamps = EnumSet.noneOf(Stamp.class);

    // The card this kill belongs to, or null for an ordinary kill.
    @Column(name = "card_id")
    private Long cardId;

    public StoredKill(long steamId64, long matchId, int roundNumber, int tick, WeaponGroup weaponGroup,
                      int difficulty, int damage, String mapArea, Set<Stamp> stamps, Long cardId) {
        this.steamId64 = steamId64;
        this.matchId = matchId;
        this.roundNumber = roundNumber;
        this.tick = tick;
        this.weaponGroup = weaponGroup;
        this.difficulty = difficulty;
        this.damage = damage;
        this.mapArea = mapArea;
        this.stamps = stamps.isEmpty() ? EnumSet.noneOf(Stamp.class) : EnumSet.copyOf(stamps);
        this.cardId = cardId;
    }
}
