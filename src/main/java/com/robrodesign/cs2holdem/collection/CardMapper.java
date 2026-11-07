package com.robrodesign.cs2holdem.collection;

import com.robrodesign.cs2holdem.hand.Event;
import com.robrodesign.cs2holdem.hand.Kill;
import com.robrodesign.cs2holdem.hand.RoundKill;

import java.util.Comparator;
import java.util.List;

/**
 * Translates between the database and the hand engine, in the same way as PokemonMapper in the
 * Pokémon Battler translated between the JSON file and the model.
 */
public final class CardMapper {

    private CardMapper() {
    }

    public static Kill toKill(StoredKill stored) {
        return new Kill(stored.getWeaponGroup(), stored.getDifficulty(), stored.getDamage(),
                stored.getMapArea(), stored.getStamps());
    }

    public static RoundKill toRoundKill(StoredKill stored) {
        return new RoundKill(stored.getMatchId(), stored.getRoundNumber(), stored.getTick(), toKill(stored));
    }

    public static Event toEvent(StoredCard card, List<StoredKill> kills) {
        List<Kill> eventKills = kills.stream()
                .sorted(Comparator.comparingInt(StoredKill::getTick))
                .map(CardMapper::toKill)
                .toList();
        return new Event(card.getType(), card.getMatchId(), card.getRoundNumber(), eventKills, card.getStamps());
    }
}
