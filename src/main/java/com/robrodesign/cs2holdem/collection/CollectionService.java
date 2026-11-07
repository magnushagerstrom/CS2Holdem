package com.robrodesign.cs2holdem.collection;

import com.robrodesign.cs2holdem.hand.Card;
import com.robrodesign.cs2holdem.hand.Event;
import com.robrodesign.cs2holdem.hand.RoundKill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Loads a player's cards and kills from the database and turns them into hand engine objects. */
@Service
public class CollectionService {

    private final StoredCardRepository cardRepository;
    private final StoredKillRepository killRepository;

    public CollectionService(StoredCardRepository cardRepository, StoredKillRepository killRepository) {
        this.cardRepository = cardRepository;
        this.killRepository = killRepository;
    }

    @Transactional(readOnly = true)
    public PlayerCollection load(long steamId64, Instant from, Instant to) {
        List<StoredCard> cards = cardRepository.findForPlayerBetween(steamId64, from, to);
        List<StoredKill> kills = killRepository.findForPlayerBetween(steamId64, from, to);

        Map<Long, List<StoredKill>> killsByCard = new HashMap<>();
        for (StoredKill kill : kills) {
            if (kill.getCardId() != null) {
                killsByCard.computeIfAbsent(kill.getCardId(), id -> new ArrayList<>()).add(kill);
            }
        }

        List<Event> events = new ArrayList<>();
        List<RoundKill> roundKills = new ArrayList<>();
        Map<Card, Long> ids = new IdentityHashMap<>();
        Map<Long, Event> eventsById = new HashMap<>();
        Map<Long, RoundKill> killsById = new HashMap<>();

        for (StoredCard card : cards) {
            Event event = CardMapper.toEvent(card, killsByCard.getOrDefault(card.getId(), List.of()));
            events.add(event);
            ids.put(event, card.getId());
            eventsById.put(card.getId(), event);
        }
        for (StoredKill kill : kills) {
            RoundKill roundKill = CardMapper.toRoundKill(kill);
            roundKills.add(roundKill);
            ids.put(roundKill, kill.getId());
            killsById.put(kill.getId(), roundKill);
        }
        return new PlayerCollection(events, roundKills, ids, eventsById, killsById);
    }
}
