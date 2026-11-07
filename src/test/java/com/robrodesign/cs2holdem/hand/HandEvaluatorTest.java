package com.robrodesign.cs2holdem.hand;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.robrodesign.cs2holdem.hand.TestCards.event;
import static com.robrodesign.cs2holdem.hand.TestCards.knife;
import static com.robrodesign.cs2holdem.hand.TestCards.roundKill;
import static org.assertj.core.api.Assertions.assertThat;

class HandEvaluatorTest {

    private static HandType typeOf(List<? extends Card> cards) {
        return HandEvaluator.evaluate(cards).orElseThrow().type();
    }

    // ---------- season hands

    @Test
    void knifeKillIsHighCard() {
        assertThat(typeOf(List.of(knife(1)))).isEqualTo(HandType.HIGH_CARD);
    }

    @Test
    void doubleKillIsPair() {
        assertThat(typeOf(List.of(event(EventType.DOUBLE_KILL, 1)))).isEqualTo(HandType.PAIR);
    }

    @Test
    void pairWithKickersIsStillPair() {
        assertThat(typeOf(List.of(event(EventType.DOUBLE_KILL, 1), knife(2), knife(3))))
                .isEqualTo(HandType.PAIR);
    }

    @Test
    void twoDoubleKillsAreTwoPair() {
        assertThat(typeOf(List.of(event(EventType.DOUBLE_KILL, 1), event(EventType.DOUBLE_KILL, 2))))
                .isEqualTo(HandType.TWO_PAIR);
    }

    @Test
    void tripleKillIsThreeOfAKind() {
        assertThat(typeOf(List.of(event(EventType.TRIPLE_KILL, 1)))).isEqualTo(HandType.THREE_OF_A_KIND);
    }

    @Test
    void tripleAndDoubleKillAreFullHouse() {
        assertThat(typeOf(List.of(event(EventType.TRIPLE_KILL, 1), event(EventType.DOUBLE_KILL, 2))))
                .isEqualTo(HandType.FULL_HOUSE);
    }

    @Test
    void quadKillIsFourOfAKind() {
        assertThat(typeOf(List.of(event(EventType.QUAD_KILL, 1), knife(2)))).isEqualTo(HandType.FOUR_OF_A_KIND);
    }

    @Test
    void aceIsFiveOfAKind() {
        Event ace = new Event(EventType.ACE, TestCards.MATCH, 1,
                List.of(TestCards.rifle(), TestCards.rifle(), TestCards.rifle(), TestCards.rifle(),
                        new Kill(WeaponGroup.RIFLE, 120, 90, "Mid")));

        assertThat(typeOf(List.of(ace))).isEqualTo(HandType.FIVE_OF_A_KIND);
    }

    @Test
    void aceWithFiveHeadshotsIsRoyalFlushNotFiveOfAKind() {
        assertThat(typeOf(List.of(event(EventType.ACE, 1, Stamp.HEADSHOT)))).isEqualTo(HandType.ROYAL_FLUSH);
    }

    @Test
    void moreThanFiveKillsIsNotAHand() {
        assertThat(HandEvaluator.evaluate(List.of(event(EventType.TRIPLE_KILL, 1),
                event(EventType.DOUBLE_KILL, 2), knife(3)))).isEmpty();
    }

    @Test
    void twoEventsFromTheSameRoundAreNotAHand() {
        assertThat(HandEvaluator.evaluate(List.of(event(EventType.DOUBLE_KILL, 4),
                event(EventType.DOUBLE_KILL, 4)))).isEmpty();
    }

    // ---------- streak hands

    @Test
    void fiveRoundsInARowWithFiveWeaponGroupsIsStraight() {
        List<RoundKill> kills = List.of(
                roundKill(7, WeaponGroup.RIFLE, "Mid"),
                roundKill(8, WeaponGroup.PISTOL, "A Site"),
                roundKill(9, WeaponGroup.SNIPER_RIFLE, "Mid"),
                roundKill(10, WeaponGroup.GRENADE, "B Site"),
                roundKill(11, WeaponGroup.MELEE, "Mid"));

        assertThat(typeOf(kills)).isEqualTo(HandType.STRAIGHT);
    }

    @Test
    void sameWeaponGroupTwiceIsNotAStraight() {
        List<RoundKill> kills = List.of(
                roundKill(7, WeaponGroup.RIFLE, "Mid"),
                roundKill(8, WeaponGroup.RIFLE, "A Site"),
                roundKill(9, WeaponGroup.SNIPER_RIFLE, "Mid"),
                roundKill(10, WeaponGroup.GRENADE, "B Site"),
                roundKill(11, WeaponGroup.MELEE, "Mid"));

        assertThat(HandEvaluator.evaluate(kills)).isEmpty();
    }

    @Test
    void roundsWithAGapAreNotAStraight() {
        List<RoundKill> kills = List.of(
                roundKill(7, WeaponGroup.RIFLE, "Mid"),
                roundKill(8, WeaponGroup.PISTOL, "Mid"),
                roundKill(9, WeaponGroup.SNIPER_RIFLE, "Mid"),
                roundKill(10, WeaponGroup.GRENADE, "Mid"),
                roundKill(12, WeaponGroup.MELEE, "A Site"));

        assertThat(HandEvaluator.evaluate(kills)).isEmpty();
    }

    @Test
    void fiveKillsInTheSameAreaWithinFiveRoundsIsFlush() {
        List<RoundKill> kills = List.of(
                roundKill(3, WeaponGroup.RIFLE, "Mid"),
                roundKill(3, WeaponGroup.RIFLE, "Mid"),
                roundKill(4, WeaponGroup.RIFLE, "Mid"),
                roundKill(6, WeaponGroup.PISTOL, "Mid"),
                roundKill(7, WeaponGroup.RIFLE, "Mid"));

        assertThat(typeOf(kills)).isEqualTo(HandType.FLUSH);
    }

    @Test
    void unknownAreaNeverFormsAFlush() {
        List<RoundKill> kills = List.of(
                roundKill(3, WeaponGroup.RIFLE, Kill.UNKNOWN_AREA),
                roundKill(3, WeaponGroup.RIFLE, Kill.UNKNOWN_AREA),
                roundKill(4, WeaponGroup.RIFLE, Kill.UNKNOWN_AREA),
                roundKill(5, WeaponGroup.RIFLE, Kill.UNKNOWN_AREA),
                roundKill(6, WeaponGroup.RIFLE, Kill.UNKNOWN_AREA));

        assertThat(HandEvaluator.evaluate(kills)).isEmpty();
    }

    @Test
    void straightInOneAreaIsStraightFlush() {
        List<RoundKill> kills = List.of(
                roundKill(7, WeaponGroup.RIFLE, "Mid"),
                roundKill(8, WeaponGroup.PISTOL, "Mid"),
                roundKill(9, WeaponGroup.SNIPER_RIFLE, "Mid"),
                roundKill(10, WeaponGroup.GRENADE, "Mid"),
                roundKill(11, WeaponGroup.MELEE, "Mid"));

        assertThat(typeOf(kills)).isEqualTo(HandType.STRAIGHT_FLUSH);
    }

    @Test
    void eventsAndRoundKillsCannotBeMixed() {
        assertThat(HandEvaluator.evaluate(List.of(event(EventType.DOUBLE_KILL, 1),
                roundKill(2, WeaponGroup.RIFLE, "Mid")))).isEmpty();
    }
}
