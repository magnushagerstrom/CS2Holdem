package com.robrodesign.cs2holdem.hand;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record Hand(HandType type, List<Event> events) {

    public static final int MAX_KILLS = 5;
    public static final int STAMP_MULTIPLIER = 4;

    public Hand {
        if (type == null) {
            throw new IllegalArgumentException("Hand type is required");
        }
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("A hand needs at least one event");
        }

        int kills = 0;
        Set<String> rounds = new HashSet<>();
        for (Event event : events) {
            kills += event.kills().size();
            String round = event.matchId() + ":" + event.round();
            if (!rounds.add(round)) {
                throw new IllegalArgumentException("Two events come from the same round: " + round);
            }
        }
        if (kills > MAX_KILLS) {
            throw new IllegalArgumentException("A hand can hold at most " + MAX_KILLS + " kills, got " + kills);
        }

        events = List.copyOf(events);
    }

    public int killCount() {
        int total = 0;
        for (Event event : events) {
            total += event.kills().size();
        }
        return total;
    }

    public int totalDamage() {
        int total = 0;
        for (Event event : events) {
            total += event.totalDamage();
        }
        return total;
    }

    public int score() {
        int killPoints = 0;
        int stampPoints = 0;
        for (Event event : events) {
            killPoints += event.killPoints();
            stampPoints += event.stampPoints();
        }
        return type.basePoints() + killPoints + stampPoints * STAMP_MULTIPLIER;
    }
}
