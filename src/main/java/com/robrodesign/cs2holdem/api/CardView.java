package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.hand.Event;
import com.robrodesign.cs2holdem.hand.Stamp;

import java.util.List;

/** A card (an event) as the API shows it. 64-bit ids are strings, see RegisterRequest. */
public record CardView(String id, String type, String typeName, String matchId, int round,
                       List<String> stamps, List<KillView> kills, int totalDamage, int points) {

    public static CardView from(long id, Event event) {
        return new CardView(
                String.valueOf(id),
                event.type().name(),
                event.type().displayName(),
                String.valueOf(event.matchId()),
                event.round(),
                event.stamps().stream().map(Stamp::displayName).sorted().toList(),
                event.kills().stream().map(KillView::from).toList(),
                event.totalDamage(),
                event.value());
    }
}
