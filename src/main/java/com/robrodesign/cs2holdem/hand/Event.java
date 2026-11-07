package com.robrodesign.cs2holdem.hand;

import java.util.List;
import java.util.Set;

public record Event(
        EventType type,
        long matchId,
        int round,
        List<Kill> kills,
        Set<Stamp> stamps
) {

    public static final int PERFECT_ACE_DAMAGE = 500;

    public Event {
        if (type == null) {
            throw new IllegalArgumentException("Event type is required");
        }
        if (round < 1) {
            throw new IllegalArgumentException("Round must be 1 or higher, was " + round);
        }
        if (kills == null || kills.size() != type.killCount()) {
            throw new IllegalArgumentException(
                    type.displayName() + " needs " + type.killCount() + " kills, got "
                            + (kills == null ? "none" : kills.size()));
        }
        if (type == EventType.SPECIAL_KILL && kills.get(0).weaponGroup() != WeaponGroup.MELEE) {
            throw new IllegalArgumentException("A special kill must be a knife or Zeus kill");
        }
        if (stamps == null) {
            throw new IllegalArgumentException("Stamps cannot be null, use an empty set");
        }
        for (Stamp stamp : stamps) {
            if (stamp.scope() != Stamp.Scope.EVENT) {
                throw new IllegalArgumentException(stamp + " is not an event stamp");
            }
        }
        kills = List.copyOf(kills);
        stamps = Set.copyOf(stamps);
    }

    public Event(EventType type, long matchId, int round, List<Kill> kills) {
        this(type, matchId, round, kills, Set.of());
    }

    public int totalDamage() {
        int total = 0;
        for (Kill kill : kills) {
            total += kill.damage();
        }
        return total;
    }

    public int killPoints() {
        int total = 0;
        for (Kill kill : kills) {
            total += kill.killPoints();
        }
        return total;
    }

    public int stampPoints() {
        int total = 0;
        for (Stamp stamp : stamps) {
            total += stamp.points();
        }
        for (Kill kill : kills) {
            total += kill.stampPoints();
        }
        return total;
    }

    public boolean isPerfectAce() {
        if (type != EventType.ACE) {
            return false;
        }
        if (totalDamage() >= PERFECT_ACE_DAMAGE) {
            return true;
        }
        for (Kill kill : kills) {
            if (!kill.stamps().contains(Stamp.HEADSHOT)) {
                return false;
            }
        }
        return true;
    }
}