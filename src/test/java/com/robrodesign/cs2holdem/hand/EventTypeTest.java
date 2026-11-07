package com.robrodesign.cs2holdem.hand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventTypeTest {

    @Test
    void eachEventTypeHasOneKillMoreThanTheOneBefore() {
        EventType[] eventTypes = EventType.values();

        for (int i = 1; i < eventTypes.length; i++) {
            EventType current = eventTypes[i];
            EventType previous = eventTypes[i - 1];

            assertThat(current.killCount()).isEqualTo(previous.killCount() + 1);
        }
    }
}