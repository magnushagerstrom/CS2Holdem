package com.robrodesign.cs2holdem.hand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KillTest {

    // ---------- killPoints

    @Test
    void killPointsAreDamageTimesDifficulty() {
        Kill kill = new Kill(WeaponGroup.PISTOL, 170, 100, "Mid");

        assertThat(kill.killPoints()).isEqualTo(17);
    }

    @Test
    void killPointsAreRoundedDown() {
        Kill kill = new Kill(WeaponGroup.RIFLE, 110, 85, "A Site");

        assertThat(kill.killPoints()).isEqualTo(9); // 9350 / 1000 = 9.35
    }

    // ---------- the limits themselves are allowed

    @Test
    void acceptsLowestAndHighestValues() {
        Kill lowest = new Kill(WeaponGroup.RIFLE, 100, 0, "Mid");
        Kill highest = new Kill(WeaponGroup.MELEE, 200, 100, "Mid");

        assertThat(lowest.difficulty()).isEqualTo(100);
        assertThat(highest.damage()).isEqualTo(100);
    }

    // ---------- invalid kills are stopped

    @Test
    void rejectsDifficultyBelowMinimum() {
        assertThatThrownBy(() -> new Kill(WeaponGroup.RIFLE, 99, 100, "Mid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Difficulty");
    }

    @Test
    void rejectsDifficultyAboveMaximum() {
        assertThatThrownBy(() -> new Kill(WeaponGroup.RIFLE, 201, 100, "Mid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Difficulty");
    }

    @Test
    void rejectsNegativeDamage() {
        assertThatThrownBy(() -> new Kill(WeaponGroup.RIFLE, 120, -1, "Mid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Damage");
    }

    @Test
    void rejectsDamageAboveMaximum() {
        assertThatThrownBy(() -> new Kill(WeaponGroup.RIFLE, 120, 101, "Mid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Damage");
    }

    @Test
    void rejectsMissingWeaponGroup() {
        assertThatThrownBy(() -> new Kill(null, 120, 100, "Mid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Weapon group");
    }

    @Test
    void rejectsBlankMapArea() {
        assertThatThrownBy(() -> new Kill(WeaponGroup.RIFLE, 120, 100, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map area");
    }

    @Test
    void rejectsMissingMapArea() {
        assertThatThrownBy(() -> new Kill(WeaponGroup.RIFLE, 120, 100, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map area");
    }
}