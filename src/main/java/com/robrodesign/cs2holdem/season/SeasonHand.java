package com.robrodesign.cs2holdem.season;

import com.robrodesign.cs2holdem.hand.HandType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A player's hand for one season, as stored in the database. */
@Entity
@Table(name = "season_hands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeasonHand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "steam_id64", nullable = false)
    private Long steamId64;

    @Column(name = "season_id", nullable = false)
    private Long seasonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "hand_type", length = 20, nullable = false)
    private HandType handType;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "total_damage", nullable = false)
    private int totalDamage;

    // true = the player built this hand; false = the strongest hand, chosen automatically
    @Column(name = "custom", nullable = false)
    private boolean custom;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // cascade + orphanRemoval: items are saved and deleted together with the hand
    @OneToMany(mappedBy = "seasonHand", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<SeasonHandItem> items = new ArrayList<>();

    public SeasonHand(long steamId64, long seasonId) {
        this.steamId64 = steamId64;
        this.seasonId = seasonId;
    }

    /** Replaces the hand's content. Items are given as card ids or kill ids in hand order. */
    public void replace(HandType handType, int score, int totalDamage, boolean custom, List<ItemRef> refs) {
        this.handType = handType;
        this.score = score;
        this.totalDamage = totalDamage;
        this.custom = custom;
        this.items.clear();
        int position = 0;
        for (ItemRef ref : refs) {
            items.add(new SeasonHandItem(this, position++, ref.cardId(), ref.killId()));
        }
    }

    /** Points at either a stored card or a stored kill. Exactly one of them is set. */
    public record ItemRef(Long cardId, Long killId) {
        public static ItemRef card(long id) {
            return new ItemRef(id, null);
        }

        public static ItemRef kill(long id) {
            return new ItemRef(null, id);
        }
    }
}
