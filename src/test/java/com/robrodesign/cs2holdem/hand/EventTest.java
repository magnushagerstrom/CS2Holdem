package com.robrodesign.cs2holdem.hand;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTest {

    private static final long MATCH_ID = 3852342945172685115L;

    // Hjälpmetod: skapar en AK-kill i Mid med valfri damage och valfria stämplar
    private static Kill rifleKill(int damage, Stamp... stamps) {
        return new Kill(WeaponGroup.RIFLE, 120, damage, "Mid", Set.of(stamps));
    }

    private static List<Kill> rifleKills(int count, int damage, Stamp... stamps) {
        List<Kill> kills = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            kills.add(rifleKill(damage, stamps));
        }
        return kills;
    }

    // ---------- number of kills

    @Test
    void tripleKillWithThreeKillsIsCreated() {
        Event event = new Event(EventType.TRIPLE_KILL, MATCH_ID, 7, rifleKills(3, 100));

        assertThat(event.kills()).hasSize(3);
    }

    @Test
    void rejectsTripleKillWithTwoKills() {
        assertThatThrownBy(() -> new Event(EventType.TRIPLE_KILL, MATCH_ID, 7, rifleKills(2, 100)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("needs 3 kills");
    }

    @Test
    void rejectsRoundZero() {
        assertThatThrownBy(() -> new Event(EventType.DOUBLE_KILL, MATCH_ID, 0, rifleKills(2, 100)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Round");
    }

    // ---------- special kills

    @Test
    void knifeKillIsASpecialKill() {
        Kill knife = new Kill(WeaponGroup.MELEE, 200, 100, "Mid");

        Event event = new Event(EventType.SPECIAL_KILL, MATCH_ID, 3, List.of(knife));

        assertThat(event.type()).isEqualTo(EventType.SPECIAL_KILL);
    }

    @Test
    void rejectsRifleKillAsSpecialKill() {
        assertThatThrownBy(() -> new Event(EventType.SPECIAL_KILL, MATCH_ID, 3, rifleKills(1, 100)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("knife or Zeus");
    }

    // ---------- stamps and points

    @Test
    void rejectsKillStampOnEvent() {
        assertThatThrownBy(() -> new Event(EventType.DOUBLE_KILL, MATCH_ID, 5, rifleKills(2, 100),
                Set.of(Stamp.HEADSHOT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not an event stamp");
    }

    @Test
    void stampPointsIncludeBothEventAndKillStamps() {
        Event event = new Event(EventType.DOUBLE_KILL, MATCH_ID, 5,
                rifleKills(2, 100, Stamp.HEADSHOT), Set.of(Stamp.ONE_SHOT));

        assertThat(event.stampPoints()).isEqualTo(7); // One shot 5 + 2 x Headshot 1
    }

    @Test
    void totalDamageAndKillPointsAreSummed() {
        Event event = new Event(EventType.TRIPLE_KILL, MATCH_ID, 7, rifleKills(3, 90));

        assertThat(event.totalDamage()).isEqualTo(270);
        assertThat(event.killPoints()).isEqualTo(30); // 3 x (90 x 120 / 1000 = 10)
    }

    // ---------- perfect ace (royal flush)

    @Test
    void aceWithFiveHeadshotsIsPerfect() {
        Event ace = new Event(EventType.ACE, MATCH_ID, 12, rifleKills(5, 80, Stamp.HEADSHOT));

        assertThat(ace.isPerfectAce()).isTrue();
    }

    @Test
    void aceWith500DamageIsPerfect() {
        Event ace = new Event(EventType.ACE, MATCH_ID, 12, rifleKills(5, 100));

        assertThat(ace.isPerfectAce()).isTrue();
    }

    @Test
    void ordinaryAceIsNotPerfect() {
        Event ace = new Event(EventType.ACE, MATCH_ID, 12, rifleKills(5, 90));

        assertThat(ace.isPerfectAce()).isFalse();
    }

    @Test
    void quadKillIsNeverAPerfectAce() {
        Event quad = new Event(EventType.QUAD_KILL, MATCH_ID, 12, rifleKills(4, 100, Stamp.HEADSHOT));

        assertThat(quad.isPerfectAce()).isFalse();
    }

    // ---------- immutability

    @Test
    void killsCannotBeChangedAfterTheEventIsCreated() {
        List<Kill> kills = rifleKills(2, 100);
        Event event = new Event(EventType.DOUBLE_KILL, MATCH_ID, 5, kills);

        kills.add(rifleKill(100));

        assertThat(event.kills()).hasSize(2);
    }
}
