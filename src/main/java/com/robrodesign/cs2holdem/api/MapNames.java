package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.hand.Card;
import com.robrodesign.cs2holdem.match.Match;
import com.robrodesign.cs2holdem.match.MatchRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Which map each match was played on, for showing it on cards.
 *
 * The hand engine only knows a card's match id; the map name lives in the matches table.
 * Looking it up card by card would be one database query per card (the "N+1 problem"),
 * so this asks for every match at once: one query, however many cards there are.
 */
@Component
public class MapNames {

    private final MatchRepository matchRepository;

    public MapNames(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    /** match id -> map name, for every match these cards come from. Matches not read yet are left out. */
    public Map<Long, String> of(Collection<? extends Card> cards) {
        Set<Long> matchIds = cards.stream().map(Card::matchId).collect(Collectors.toSet());
        if (matchIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        for (Match match : matchRepository.findAllById(matchIds)) {
            if (match.getMapName() != null) {
                names.put(match.getMatchId(), match.getMapName());
            }
        }
        return names;
    }
}
