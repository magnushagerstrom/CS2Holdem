package com.robrodesign.cs2holdem.hand;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A poker hand: a hand type and the cards it is made of.
 *
 * Use {@link HandEvaluator#evaluate} to find out which hand type some cards form.
 * This record only guards the rules that apply to every hand.
 */
public record Hand(HandType type, List<Card> cards) {

    public static final int MAX_KILLS = 5;
    public static final int STAMP_MULTIPLIER = 4;

    public Hand {
        if (type == null) {
            throw new IllegalArgumentException("Hand type is required");
        }
        if (cards == null || cards.isEmpty()) {
            throw new IllegalArgumentException("A hand needs at least one card");
        }

        int kills = 0;
        Set<String> eventRounds = new HashSet<>();
        for (Card card : cards) {
            kills += card.kills().size();
            if (card instanceof Event) {
                String round = card.matchId() + ":" + card.round();
                if (!eventRounds.add(round)) {
                    throw new IllegalArgumentException("Two events come from the same round: " + round);
                }
            }
        }
        if (kills > MAX_KILLS) {
            throw new IllegalArgumentException("A hand can hold at most " + MAX_KILLS + " kills, got " + kills);
        }

        cards = List.copyOf(cards);
    }

    public List<Kill> kills() {
        List<Kill> all = new ArrayList<>();
        for (Card card : cards) {
            all.addAll(card.kills());
        }
        return all;
    }

    public int killCount() {
        return kills().size();
    }

    public int totalDamage() {
        int total = 0;
        for (Card card : cards) {
            total += card.totalDamage();
        }
        return total;
    }

    public int score() {
        int total = type.basePoints();
        for (Card card : cards) {
            total += card.value();
        }
        return total;
    }
}
