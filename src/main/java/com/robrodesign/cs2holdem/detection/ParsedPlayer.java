package com.robrodesign.cs2holdem.detection;

/**
 * One player's totals for a whole match, as the demo worker adds them up (live rounds only).
 *
 * @param team          the side the player ended the match on: 2 = T, 3 = CT (Valve's numbers)
 * @param damage        health removed from enemies, never more than they had left
 * @param utilityDamage the part of the damage that came from HE grenades and fire
 * @param shotsHit      bullet hits on enemies; a shotgun blast that hits counts once per enemy hit
 */
public record ParsedPlayer(
        long steamId,
        String name,
        int team,
        int roundsPlayed,
        int damage,
        int utilityDamage,
        int assists,
        int flashAssists,
        int mvps,
        int shotsFired,
        int shotsHit
) {
    public static final int TERRORIST = 2;
    public static final int COUNTER_TERRORIST = 3;
}
