package com.robrodesign.cs2holdem.season;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One card or kill in a season hand. */
@Entity
@Table(name = "season_hand_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeasonHandItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_hand_id", nullable = false)
    private SeasonHand seasonHand;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "kill_id")
    private Long killId;

    SeasonHandItem(SeasonHand seasonHand, int position, Long cardId, Long killId) {
        this.seasonHand = seasonHand;
        this.position = position;
        this.cardId = cardId;
        this.killId = killId;
    }
}
