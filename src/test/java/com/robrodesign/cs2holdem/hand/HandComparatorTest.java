package com.robrodesign.cs2holdem.hand;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.robrodesign.cs2holdem.hand.TestCards.event;
import static org.assertj.core.api.Assertions.assertThat;

class HandComparatorTest {

    @Test
    void higherScoreWins() {
        Hand plain = new Hand(HandType.PAIR, List.of(event(EventType.DOUBLE_KILL, 1)));
        Hand smoked = new Hand(HandType.PAIR, List.of(event(EventType.DOUBLE_KILL, 2, Stamp.SMOKED)));

        assertThat(HandComparator.BY_STRENGTH.compare(smoked, plain) > 0).isTrue();
    }

    @Test
    void equalHandsCompareAsEqual() {
        Hand a = new Hand(HandType.PAIR, List.of(event(EventType.DOUBLE_KILL, 1)));
        Hand b = new Hand(HandType.PAIR, List.of(event(EventType.DOUBLE_KILL, 2)));

        assertThat(HandComparator.BY_STRENGTH.compare(a, b)).isEqualTo(0);
    }
}
