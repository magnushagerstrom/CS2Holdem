package com.robrodesign.cs2holdem.hand;

import java.util.List;

/**
 * One ordinary kill and the round it happened in. Straights and flushes are built from these,
 * since they count every kill, not only the highlight events.
 *
 * @param tick game tick of the kill, used to order kills and to replay them
 */
public record RoundKill(long matchId, int round, int tick, Kill kill) implements Card {

    public RoundKill {
        if (round < 1) {
            throw new IllegalArgumentException("Round must be 1 or higher, was " + round);
        }
        if (tick < 0) {
            throw new IllegalArgumentException("Tick cannot be negative, was " + tick);
        }
        if (kill == null) {
            throw new IllegalArgumentException("Kill is required");
        }
    }

    @Override
    public List<Kill> kills() {
        return List.of(kill);
    }

    @Override
    public int totalDamage() {
        return kill.damage();
    }

    @Override
    public int killPoints() {
        return kill.killPoints();
    }

    @Override
    public int stampPoints() {
        return kill.stampPoints();
    }
}
