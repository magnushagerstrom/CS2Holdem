package com.robrodesign.cs2holdem.hand;

import java.util.List;

/**
 * Something a hand can be built from.
 *
 * - An {@link Event} is a card from the player's collection: a double kill, an ace, a knife kill ...
 * - A {@link RoundKill} is one ordinary kill from a round. Straights and flushes are built from these.
 *
 * The interface is sealed: no other kinds of cards can exist, so code that handles cards
 * can be sure it has covered every case.
 */
public sealed interface Card permits Event, RoundKill {

    long matchId();

    int round();

    List<Kill> kills();

    int totalDamage();

    int killPoints();

    int stampPoints();

    /** What this card adds to a hand's score. */
    default int value() {
        return killPoints() + stampPoints() * Hand.STAMP_MULTIPLIER;
    }
}
