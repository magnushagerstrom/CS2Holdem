package com.robrodesign.cs2holdem.hand;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Decides which hand type some cards form. All rules come from docs/CONCEPT.md.
 *
 * There are two families of hands:
 * - season hands, built from events collected during the whole season (pair, full house, ace ...)
 * - streak hands, built from ordinary kills within five rounds in a row of one match
 *   (straight, flush, straight flush)
 */
public final class HandEvaluator {

    public static final int STREAK_ROUNDS = 5;

    private HandEvaluator() {
    }

    /** Returns the hand the cards form, or empty if they do not form a valid hand. */
    public static Optional<Hand> evaluate(List<? extends Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return Optional.empty();
        }
        boolean allEvents = cards.stream().allMatch(card -> card instanceof Event);
        boolean allRoundKills = cards.stream().allMatch(card -> card instanceof RoundKill);

        Optional<HandType> type;
        if (allEvents) {
            type = evaluateEvents(cards.stream().map(card -> (Event) card).toList());
        } else if (allRoundKills) {
            type = evaluateStreak(cards.stream().map(card -> (RoundKill) card).toList());
        } else {
            type = Optional.empty(); // events and ordinary kills cannot be mixed in one hand
        }
        return type.map(handType -> new Hand(handType, List.copyOf(cards)));
    }

    // ---------------------------------------------------------------- season hands

    static Optional<HandType> evaluateEvents(List<Event> events) {
        int kills = 0;
        Set<String> rounds = new HashSet<>();
        Map<EventType, Integer> counts = new EnumMap<>(EventType.class);
        for (Event event : events) {
            kills += event.kills().size();
            if (!rounds.add(event.matchId() + ":" + event.round())) {
                return Optional.empty();
            }
            counts.merge(event.type(), 1, Integer::sum);
        }
        if (kills > Hand.MAX_KILLS) {
            return Optional.empty();
        }

        int aces = counts.getOrDefault(EventType.ACE, 0);
        int quads = counts.getOrDefault(EventType.QUAD_KILL, 0);
        int triples = counts.getOrDefault(EventType.TRIPLE_KILL, 0);
        int doubles = counts.getOrDefault(EventType.DOUBLE_KILL, 0);

        // With at most five kills, an ace is always alone and a quad has at most one kicker.
        if (aces == 1) {
            return Optional.of(events.get(0).isPerfectAce() ? HandType.ROYAL_FLUSH : HandType.FIVE_OF_A_KIND);
        }
        if (quads == 1) {
            return Optional.of(HandType.FOUR_OF_A_KIND);
        }
        if (triples == 1 && doubles == 1) {
            return Optional.of(HandType.FULL_HOUSE);
        }
        if (triples == 1) {
            return Optional.of(HandType.THREE_OF_A_KIND);
        }
        if (doubles == 2) {
            return Optional.of(HandType.TWO_PAIR);
        }
        if (doubles == 1) {
            return Optional.of(HandType.PAIR);
        }
        return Optional.of(HandType.HIGH_CARD); // only special kills left
    }

    // ---------------------------------------------------------------- streak hands

    static Optional<HandType> evaluateStreak(List<RoundKill> kills) {
        if (kills.size() != Hand.MAX_KILLS) {
            return Optional.empty();
        }
        long matchId = kills.get(0).matchId();
        int firstRound = Integer.MAX_VALUE;
        int lastRound = Integer.MIN_VALUE;
        for (RoundKill kill : kills) {
            if (kill.matchId() != matchId) {
                return Optional.empty();
            }
            firstRound = Math.min(firstRound, kill.round());
            lastRound = Math.max(lastRound, kill.round());
        }
        if (lastRound - firstRound >= STREAK_ROUNDS) {
            return Optional.empty();
        }

        boolean straight = isStraight(kills);
        boolean flush = isFlush(kills);
        if (straight && flush) {
            return Optional.of(HandType.STRAIGHT_FLUSH);
        }
        if (flush) {
            return Optional.of(HandType.FLUSH);
        }
        if (straight) {
            return Optional.of(HandType.STRAIGHT);
        }
        return Optional.empty();
    }

    /** Five different rounds in a row, one kill each, five different weapon groups. */
    static boolean isStraight(List<RoundKill> kills) {
        Set<Integer> rounds = new HashSet<>();
        Set<WeaponGroup> groups = EnumSet.noneOf(WeaponGroup.class);
        for (RoundKill kill : kills) {
            rounds.add(kill.round());
            groups.add(kill.kill().weaponGroup());
        }
        // Five different rounds within a five-round window are always five rounds in a row.
        return rounds.size() == Hand.MAX_KILLS && groups.size() == Hand.MAX_KILLS;
    }

    /** All five kills in the same known area of the map. */
    static boolean isFlush(List<RoundKill> kills) {
        String area = kills.get(0).kill().mapArea();
        if (Kill.UNKNOWN_AREA.equals(area)) {
            return false;
        }
        for (RoundKill kill : kills) {
            if (!area.equals(kill.kill().mapArea())) {
                return false;
            }
        }
        return true;
    }
}
