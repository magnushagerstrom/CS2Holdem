package com.robrodesign.cs2holdem.hand;

public record Kill(
        WeaponGroup weaponGroup,
        int difficulty,
        int damage,
        String mapArea
) {

    public static final int MIN_DIFFICULTY = 100;
    public static final int MAX_DIFFICULTY = 200;
    public static final int MAX_DAMAGE = 100;

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
                    "Damage must be between 0 and " + MAX_DAMAGE + ",  was " + damage);
        }
        if (mapArea == null || mapArea.isBlank()) {
            throw new IllegalArgumentException("Map area is required");
        }
    }

    public int killPoints() {
        return damage * difficulty / 1000;
    }
}