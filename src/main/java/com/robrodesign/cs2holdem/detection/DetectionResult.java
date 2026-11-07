package com.robrodesign.cs2holdem.detection;

import com.robrodesign.cs2holdem.hand.Event;
import com.robrodesign.cs2holdem.hand.RoundKill;

import java.util.List;

/**
 * What one player got out of one match.
 *
 * @param events     the cards: highlight events such as a double kill or an ace
 * @param roundKills every counted kill, used for straights and flushes
 */
public record DetectionResult(long steamId64, List<Event> events, List<RoundKill> roundKills) {
    public DetectionResult {
        events = List.copyOf(events);
        roundKills = List.copyOf(roundKills);
    }
}
