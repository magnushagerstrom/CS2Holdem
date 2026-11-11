package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.hand.Event;
import com.robrodesign.cs2holdem.hand.RoundKill;
import com.robrodesign.cs2holdem.season.SeasonHandService.HandDetails;
import com.robrodesign.cs2holdem.season.SeasonHandService.HandItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A season hand. Each item is either a card or, for straights and flushes, an ordinary kill.
 */
public record HandView(SeasonView season, String type, String typeName, int basePoints, int score,
                       int totalDamage, boolean custom, List<Item> items) {

    public record Item(String kind, CardView card, KillView kill) {
    }

    public static HandView from(HandDetails details, Map<Long, String> maps) {
        List<Item> items = new ArrayList<>();
        for (HandItem item : details.items()) {
            if (item.card() instanceof Event event) {
                items.add(new Item("CARD", CardView.from(item.id(), event, maps), null));
            } else if (item.card() instanceof RoundKill kill) {
                items.add(new Item("KILL", null, KillView.from(item.id(), kill, maps)));
            }
        }
        return new HandView(
                SeasonView.from(details.season()),
                details.hand().type().name(),
                details.hand().type().displayName(),
                details.hand().type().basePoints(),
                details.hand().score(),
                details.hand().totalDamage(),
                details.stored().isCustom(),
                items);
    }
}
