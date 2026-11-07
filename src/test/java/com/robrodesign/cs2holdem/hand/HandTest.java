package com.robrodesign.cs2holdem.hand;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HandTest {

    private static final long MATCH_A = 1001L;
    private static final long MATCH_B = 2002L;

    // Hjälpmetod: en händelse med AK-kills i Mid, 100 damage per kill
    private static Event rifleEvent(EventType type, long matchId, int round) {
        List<Kill> kills = new ArrayList<>();
        for (int i = 0; i < type.killCount(); i++) {
            kills.add(new Kill(WeaponGroup.RIFLE, 120, 100, "Mid"));
        }
        return new Event(type, matchId, round, kills);
    }

    private static Event knifeKill(long matchId, int round) {
        Kill knife = new Kill(WeaponGroup.MELEE, 200, 100, "Mid");
        return new Event(EventType.SPECIAL_KILL, matchId, round, List.of(knife));
    }

    // ---------- score

    @Test
    void scoreIsBasePointsPlusKillPointsPlusStampPoints() {
        // Tre no-scope-AWP-kills genom smoke i samma runda
        List<Kill> kills = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            kills.add(new Kill(WeaponGroup.SNIPER_RIFLE, 180, 100, "Mid",
                    Set.of(Stamp.NO_SCOPE, Stamp.SMOKED)));
        }
        Event triple = new Event(EventType.TRIPLE_KILL, MATCH_A, 9, kills);

        Hand hand = new Hand(HandType.THREE_OF_A_KIND, List.of(triple));

        // 50 bas + 3 x 18 kill-poäng + 3 x (5 + 2) stämpelpoäng x 4
        assertThat(hand.score()).isEqualTo(50 + 54 + 84);
    }

    @Test
    void ordinaryFullHouseScore() {
        Hand hand = new Hand(HandType.FULL_HOUSE, List.of(
                rifleEvent(EventType.TRIPLE_KILL, MATCH_A, 3),
                rifleEvent(EventType.DOUBLE_KILL, MATCH_B, 11)));

        // 110 bas + 5 x 12 kill-poäng, inga stämplar
        assertThat(hand.score()).isEqualTo(170);
    }

    @Test
    void totalDamageAndKillCountAreSummed() {
        Hand hand = new Hand(HandType.FULL_HOUSE, List.of(
                rifleEvent(EventType.TRIPLE_KILL, MATCH_A, 3),
                rifleEvent(EventType.DOUBLE_KILL, MATCH_A, 4)));

        assertThat(hand.killCount()).isEqualTo(5);
        assertThat(hand.totalDamage()).isEqualTo(500);
    }

    // ---------- rules

    @Test
    void rejectsMoreThanFiveKills() {
        assertThatThrownBy(() -> new Hand(HandType.FULL_HOUSE, List.of(
                rifleEvent(EventType.TRIPLE_KILL, MATCH_A, 3),
                rifleEvent(EventType.DOUBLE_KILL, MATCH_A, 4),
                knifeKill(MATCH_A, 5))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 5 kills");
    }

    @Test
    void rejectsTwoEventsFromTheSameRound() {
        assertThatThrownBy(() -> new Hand(HandType.TWO_PAIR, List.of(
                rifleEvent(EventType.DOUBLE_KILL, MATCH_A, 6),
                rifleEvent(EventType.DOUBLE_KILL, MATCH_A, 6))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same round");
    }

    @Test
    void sameRoundNumberInDifferentMatchesIsAllowed() {
        Hand hand = new Hand(HandType.TWO_PAIR, List.of(
                rifleEvent(EventType.DOUBLE_KILL, MATCH_A, 6),
                rifleEvent(EventType.DOUBLE_KILL, MATCH_B, 6)));

        assertThat(hand.events()).hasSize(2);
    }

    @Test
    void rejectsEmptyHand() {
        assertThatThrownBy(() -> new Hand(HandType.HIGH_CARD, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one event");
    }

    @Test
    void rejectsMissingHandType() {
        assertThatThrownBy(() -> new Hand(null, List.of(knifeKill(MATCH_A, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hand type");
    }

    // ---------- immutability

    @Test
    void eventsCannotBeChangedAfterTheHandIsCreated() {
        List<Event> events = new ArrayList<>();
        events.add(rifleEvent(EventType.DOUBLE_KILL, MATCH_A, 2));
        Hand hand = new Hand(HandType.PAIR, events);

        events.add(knifeKill(MATCH_A, 3));

        assertThat(hand.events()).hasSize(1);
    }
}
