package com.robrodesign.cs2holdem.hand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Finds the strongest hand a player can build from everything they collected in a season.
 *
 * Trying every combination of hundreds of cards would take far too long. Instead the finder
 * uses the fact that a hand's score is a sum: base points plus what each card adds. For every
 * hand type the best hand is therefore the best card for each slot, and the strongest hand is
 * the best of those few candidates.
 */
public final class BestHandFinder {

    private static final Comparator<Card> BEST_FIRST = Comparator
            .comparingInt(Card::value)
            .thenComparingInt(Card::totalDamage)
            .reversed();

    private BestHandFinder() {
    }

    /**
     * @param events     the player's cards from the season
     * @param roundKills every counted kill the player made in the season, for straights and flushes
     */
    public static Optional<Hand> find(List<Event> events, List<RoundKill> roundKills) {
        List<Hand> candidates = new ArrayList<>();
        candidates.addAll(seasonCandidates(events));
        candidates.addAll(streakCandidates(roundKills));
        return candidates.stream().max(HandComparator.BY_STRENGTH);
    }

    // ---------------------------------------------------------------- season hands

    static List<Hand> seasonCandidates(List<Event> events) {
        List<Event> perfectAces = new ArrayList<>();
        List<Event> aces = new ArrayList<>();
        List<Event> quads = new ArrayList<>();
        List<Event> triples = new ArrayList<>();
        List<Event> doubles = new ArrayList<>();
        List<Event> specials = new ArrayList<>();
        for (Event event : events) {
            switch (event.type()) {
                case ACE -> (event.isPerfectAce() ? perfectAces : aces).add(event);
                case QUAD_KILL -> quads.add(event);
                case TRIPLE_KILL -> triples.add(event);
                case DOUBLE_KILL -> doubles.add(event);
                case SPECIAL_KILL -> specials.add(event);
            }
        }
        for (List<Event> list : List.of(perfectAces, aces, quads, triples, doubles, specials)) {
            list.sort(BEST_FIRST);
        }

        List<Hand> hands = new ArrayList<>();
        add(hands, top(perfectAces, 1));
        add(hands, top(aces, 1));
        add(hands, top(quads, 1), top(specials, 1));
        add(hands, top(triples, 1), top(doubles, 1));
        add(hands, top(triples, 1), top(specials, 2));
        add(hands, top(doubles, 2), top(specials, 1));
        add(hands, top(doubles, 1), top(specials, 3));
        add(hands, top(specials, 5));
        return hands;
    }

    /** Adds a hand if the required cards (the first list) exist. Kickers are optional extras. */
    @SafeVarargs
    private static void add(List<Hand> hands, List<Event> required, List<Event>... kickers) {
        if (required.isEmpty()) {
            return;
        }
        List<Event> cards = new ArrayList<>(required);
        for (List<Event> kicker : kickers) {
            cards.addAll(kicker);
        }
        HandEvaluator.evaluate(cards).ifPresent(hands::add);
    }

    /** The best n cards, or an empty list if there are fewer than n required. */
    private static List<Event> top(List<Event> sorted, int n) {
        return sorted.subList(0, Math.min(n, sorted.size()));
    }

    // ---------------------------------------------------------------- streak hands

    static List<Hand> streakCandidates(List<RoundKill> roundKills) {
        Map<Long, TreeMap<Integer, List<RoundKill>>> byMatch = new HashMap<>();
        for (RoundKill kill : roundKills) {
            byMatch.computeIfAbsent(kill.matchId(), id -> new TreeMap<>())
                    .computeIfAbsent(kill.round(), round -> new ArrayList<>())
                    .add(kill);
        }

        List<Hand> hands = new ArrayList<>();
        for (TreeMap<Integer, List<RoundKill>> rounds : byMatch.values()) {
            int first = rounds.firstKey();
            int last = rounds.lastKey();
            for (int start = first; start <= last; start++) {
                List<List<RoundKill>> window = new ArrayList<>();
                for (int round = start; round < start + HandEvaluator.STREAK_ROUNDS; round++) {
                    window.add(rounds.getOrDefault(round, List.of()));
                }
                bestStraights(window).forEach(hands::add);
                bestFlush(window).ifPresent(hands::add);
            }
        }
        return hands;
    }

    /** The best straight and the best straight flush in a window, by trying one kill per round. */
    private static List<Hand> bestStraights(List<List<RoundKill>> window) {
        for (List<RoundKill> round : window) {
            if (round.isEmpty()) {
                return List.of(); // a straight needs a kill in every round
            }
        }
        Hand[] best = new Hand[2]; // [0] straight, [1] straight flush
        search(window, 0, new ArrayList<>(), EnumSet.noneOf(WeaponGroup.class), best);
        List<Hand> found = new ArrayList<>();
        for (Hand hand : best) {
            if (hand != null) {
                found.add(hand);
            }
        }
        return found;
    }

    private static void search(List<List<RoundKill>> window, int index, List<RoundKill> chosen,
                               Set<WeaponGroup> usedGroups, Hand[] best) {
        if (index == window.size()) {
            HandEvaluator.evaluate(chosen).ifPresent(hand -> {
                int slot = hand.type() == HandType.STRAIGHT_FLUSH ? 1 : 0;
                if (best[slot] == null || HandComparator.BY_STRENGTH.compare(hand, best[slot]) > 0) {
                    best[slot] = hand;
                }
            });
            return;
        }
        for (RoundKill kill : window.get(index)) {
            WeaponGroup group = kill.kill().weaponGroup();
            if (usedGroups.contains(group)) {
                continue; // five different weapon groups are required
            }
            chosen.add(kill);
            usedGroups.add(group);
            search(window, index + 1, chosen, usedGroups, best);
            usedGroups.remove(group);
            chosen.remove(chosen.size() - 1);
        }
    }

    /** The best flush in a window: the five most valuable kills in the same area. */
    private static Optional<Hand> bestFlush(List<List<RoundKill>> window) {
        Map<String, List<RoundKill>> byArea = new HashMap<>();
        for (List<RoundKill> round : window) {
            for (RoundKill kill : round) {
                String area = kill.kill().mapArea();
                if (!Kill.UNKNOWN_AREA.equals(area)) {
                    byArea.computeIfAbsent(area, a -> new ArrayList<>()).add(kill);
                }
            }
        }
        Optional<Hand> best = Optional.empty();
        for (List<RoundKill> kills : byArea.values()) {
            if (kills.size() < Hand.MAX_KILLS) {
                continue;
            }
            kills.sort(BEST_FIRST);
            Optional<Hand> hand = HandEvaluator.evaluate(kills.subList(0, Hand.MAX_KILLS));
            if (hand.isPresent() && (best.isEmpty()
                    || HandComparator.BY_STRENGTH.compare(hand.get(), best.get()) > 0)) {
                best = hand;
            }
        }
        return best;
    }
}
