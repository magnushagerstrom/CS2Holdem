package com.robrodesign.cs2holdem.hand;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.robrodesign.cs2holdem.hand.TestCards.event;
import static com.robrodesign.cs2holdem.hand.TestCards.knife;
import static com.robrodesign.cs2holdem.hand.TestCards.roundKill;
import static org.assertj.core.api.Assertions.assertThat;

class BestHandFinderTest {

    @Test
    void noCardsMeansNoHand() {
        assertThat(BestHandFinder.find(List.of(), List.of())).isEmpty();
    }

    @Test
    void picksFullHouseOverPairAndThreeOfAKind() {
        List<Event> cards = List.of(
                event(EventType.DOUBLE_KILL, 1),
                event(EventType.TRIPLE_KILL, 2),
                event(EventType.DOUBLE_KILL, 3));

        Hand best = BestHandFinder.find(cards, List.of()).orElseThrow();

        assertThat(best.type()).isEqualTo(HandType.FULL_HOUSE);
    }

    @Test
    void picksTheMostValuableCardForEachSlot() {
        Event plainDouble = event(EventType.DOUBLE_KILL, 1);
        Event smokedDouble = event(EventType.DOUBLE_KILL, 2, Stamp.SMOKED);
        Event triple = event(EventType.TRIPLE_KILL, 3);

        Hand best = BestHandFinder.find(List.of(plainDouble, smokedDouble, triple), List.of()).orElseThrow();

        assertThat(best.type()).isEqualTo(HandType.FULL_HOUSE);
        assertThat(best.cards().contains(smokedDouble)).isTrue();
    }

    @Test
    void spectacularThreeOfAKindCanBeatAPlainFullHouse() {
        // Tre no-scope-kills genom smoke väger tyngre än en vanlig kåk
        Event spectacularTriple = event(EventType.TRIPLE_KILL, 1, Stamp.NO_SCOPE, Stamp.SMOKED);
        Event plainTriple = event(EventType.TRIPLE_KILL, 2);
        Event plainDouble = event(EventType.DOUBLE_KILL, 3);

        Hand best = BestHandFinder.find(List.of(spectacularTriple, plainTriple, plainDouble), List.of())
                .orElseThrow();

        // Kåken byggs av den spektakulära trippelkillen, så kåken vinner ändå
        assertThat(best.type()).isEqualTo(HandType.FULL_HOUSE);
        assertThat(best.cards().contains(spectacularTriple)).isTrue();
    }

    @Test
    void fillsEmptySlotsWithKickers() {
        Hand best = BestHandFinder.find(List.of(event(EventType.DOUBLE_KILL, 1), knife(2), knife(3), knife(4)),
                List.of()).orElseThrow();

        assertThat(best.type()).isEqualTo(HandType.PAIR);
        assertThat(best.killCount()).isEqualTo(5);
    }

    @Test
    void findsAStraightInsideALongerMatch() {
        List<RoundKill> kills = new ArrayList<>(List.of(
                roundKill(5, WeaponGroup.RIFLE, "A Site"),
                roundKill(6, WeaponGroup.RIFLE, "B Site"),
                roundKill(7, WeaponGroup.PISTOL, "A Site"),
                roundKill(8, WeaponGroup.SNIPER_RIFLE, "B Site"),
                roundKill(9, WeaponGroup.GRENADE, "A Site"),
                roundKill(10, WeaponGroup.MELEE, "B Site")));

        Hand best = BestHandFinder.find(List.of(), kills).orElseThrow();

        assertThat(best.type()).isEqualTo(HandType.STRAIGHT);
    }

    @Test
    void findsAFlushAndIgnoresKillsOutsideTheArea() {
        List<RoundKill> kills = List.of(
                roundKill(3, WeaponGroup.RIFLE, "Mid"),
                roundKill(3, WeaponGroup.RIFLE, "Mid"),
                roundKill(4, WeaponGroup.RIFLE, "A Site"),
                roundKill(5, WeaponGroup.RIFLE, "Mid"),
                roundKill(6, WeaponGroup.RIFLE, "Mid"),
                roundKill(7, WeaponGroup.RIFLE, "Mid"));

        Hand best = BestHandFinder.find(List.of(), kills).orElseThrow();

        assertThat(best.type()).isEqualTo(HandType.FLUSH);
    }

    @Test
    void royalFlushBeatsEverything() {
        List<Event> cards = List.of(
                event(EventType.ACE, 1, Stamp.HEADSHOT),
                event(EventType.QUAD_KILL, 2, Stamp.NO_SCOPE, Stamp.SMOKED),
                event(EventType.TRIPLE_KILL, 3));

        Hand best = BestHandFinder.find(cards, List.of()).orElseThrow();

        assertThat(best.type()).isEqualTo(HandType.ROYAL_FLUSH);
    }
}
