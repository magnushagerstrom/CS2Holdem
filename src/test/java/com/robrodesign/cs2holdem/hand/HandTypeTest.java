package com.robrodesign.cs2holdem.hand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class HandTypeTest {

    @Test
    void eachHandTypeIsWorthMoreThanTheOneBefore() {
        HandType[] handTypes = HandType.values();

        for (int i = 1; i < handTypes.length; i++) {
            HandType current = handTypes[i];
            HandType previous = handTypes[i - 1];

            assertThat(current.basePoints()).isGreaterThan(previous.basePoints());
        }
    }

}
