package com.robrodesign.cs2holdem.collection;

import com.robrodesign.cs2holdem.hand.Card;
import com.robrodesign.cs2holdem.hand.Event;
import com.robrodesign.cs2holdem.hand.RoundKill;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Everything a player collected during a period, ready for the hand engine, plus the database id
 * behind every card and kill.
 *
 * The ids are kept in an IdentityHashMap: two kills with exactly the same values are equal records,
 * but they are still two different kills in the database, so we must look them up by the object
 * itself, not by its values.
 */
public final class PlayerCollection {

    private final List<Event> events;
    private final List<RoundKill> roundKills;
    private final Map<Card, Long> ids;
    private final Map<Long, Event> eventsById;
    private final Map<Long, RoundKill> killsById;

    PlayerCollection(List<Event> events, List<RoundKill> roundKills, Map<Card, Long> ids,
                     Map<Long, Event> eventsById, Map<Long, RoundKill> killsById) {
        this.events = List.copyOf(events);
        this.roundKills = List.copyOf(roundKills);
        this.ids = Collections.unmodifiableMap(new IdentityHashMap<>(ids));
        this.eventsById = Map.copyOf(eventsById);
        this.killsById = Map.copyOf(killsById);
    }

    public List<Event> events() {
        return events;
    }

    public List<RoundKill> roundKills() {
        return roundKills;
    }

    /** The database id of a card or kill in this collection. */
    public long idOf(Card card) {
        Long id = ids.get(card);
        if (id == null) {
            throw new IllegalArgumentException("Card is not part of this collection");
        }
        return id;
    }

    public Optional<Event> event(long cardId) {
        return Optional.ofNullable(eventsById.get(cardId));
    }

    public Optional<RoundKill> roundKill(long killId) {
        return Optional.ofNullable(killsById.get(killId));
    }
}
