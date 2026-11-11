package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.hand.Event;
import com.robrodesign.cs2holdem.hand.Stamp;

import java.util.List;
import java.util.Map;

/** A card (an event) as the API shows it. 64-bit ids are strings, see RegisterRequest. */
public record CardView(String id, String type, String typeName, String matchId, String mapName, int round,
                       List<String> stamps, List<KillView> kills, int totalDamage, int points) {

    /** maps: match id -> map name, from {@link MapNames}. The map is null until the demo is read. */
    public static CardView from(long id, Event event, Map<Long, String> maps) {
        return new CardView(
                String.valueOf(id),
                event.type().name(),
                event.type().displayName(),
                String.valueOf(event.matchId()),
                maps.get(event.matchId()),
                event.round(),
                event.stamps().stream().map(Stamp::displayName).sorted().toList(),
                event.kills().stream().map(KillView::from).toList(),
                event.totalDamage(),
                event.value());
    }
}
