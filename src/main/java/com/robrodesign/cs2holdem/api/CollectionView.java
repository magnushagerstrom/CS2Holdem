package com.robrodesign.cs2holdem.api;

import java.util.List;

/**
 * Everything a player collected in a season.
 *
 * @param cards highlight events, the cards that most hands are built from
 * @param kills every counted kill, which straights and flushes are built from
 */
public record CollectionView(SeasonView season, List<CardView> cards, List<KillView> kills) {
}
