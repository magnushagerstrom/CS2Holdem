package com.robrodesign.cs2holdem.hand;

import java.util.Set;

public record Kill(
        WeaponGroup weaponGroup,
        int difficulty,
        int damage,
        String mapArea,
        Set<Stamp> stamps
) {

    public static final int MIN_DIFFICULTY = 100;
    public static final int MAX_DIFFICULTY = 200;
    public static final int MAX_DAMAGE = 100;
    /** Used when the demo has no callout name for the position. Never forms a flush. */
    public static final String UNKNOWN_AREA = "Unknown";

    public Kill {
        if (weaponGroup == null) {
            throw new IllegalArgumentException("Weapon group is required");
        }
        if (difficulty < MIN_DIFFICULTY || difficulty > MAX_DIFFICULTY) {
            throw new IllegalArgumentException(
                    "Difficulty must be between " + MIN_DIFFICULTY + " and " + MAX_DIFFICULTY + ", was " + difficulty);
        }
        if (damage < 0 || damage > MAX_DAMAGE) {
            throw new IllegalArgumentException(
                    "Damage must be between 0 and " + MAX_DAMAGE + ", was " + damage);
        }
        if (mapArea == null || mapArea.isBlank()) {
            throw new IllegalArgumentException("Map area is required");
        }
        if (stamps == null) {
            throw new IllegalArgumentException("Stamps cannot be null, use an empty set");
        }
        for (Stamp stamp : stamps) {
            if (stamp.scope() != Stamp.Scope.KILL) {
                throw new IllegalArgumentException(stamp + " is not a kill stamp");
            }
        }
        stamps = Set.copyOf(stamps);
    }

    public Kill(WeaponGroup weaponGroup, int difficulty, int damage, String mapArea) {
        this(weaponGroup, difficulty, damage, mapArea, Set.of());
    }

    public int killPoints() {
        // difficulty is 100-200 (1.0-2.0 x 100), so this is damage x difficulty: every point of
        // damage counts. With / 1000 (damage / 10) integer division cut off everything after the
        // decimal point, and 91 and 99 damage both gave 9 points.
        return damage * difficulty / 100;
    }

    /** Kill points plus weighted stamp points: what this kill adds to a hand's score. */
    public int value() {
        return killPoints() + stampPoints() * Hand.STAMP_MULTIPLIER;
    }

    public int stampPoints() {
        int total = 0;
        for (Stamp stamp : stamps) {
            total += stamp.points();
        }
        return total;
    }
}
