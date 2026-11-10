package com.robrodesign.cs2holdem.detection;

/**
 * One kill exactly as the demo parser reports it. Nothing is filtered or interpreted yet;
 * that is the {@link EventDetector}'s job.
 *
 * @param round           round number, starting at 1
 * @param tick            game tick of the kill (CS2 runs at 64 ticks per second)
 * @param weapon          weapon name without the "weapon_" prefix, e.g. "ak47", "awp", "hegrenade"
 * @param damage          total health the killer removed from the victim during the round
 * @param grenadeId       id of the grenade entity for grenade kills, otherwise null
 * @param killerPlace     callout name of the killer's position, e.g. "Mid", or null if unknown
 * @param killerLastAlive the killer was the last player alive in their team
 * @param firstDamageTick when the killer first hurt this victim in the round, 0 if unknown.
 *                        Used for time to kill.
 */
public record ParsedKill(
        int round,
        int tick,
        boolean warmup,
        long killerSteamId,
        boolean killerIsBot,
        int killerTeam,
        long victimSteamId,
        boolean victimIsBot,
        int victimTeam,
        String weapon,
        int damage,
        boolean headshot,
        boolean wallbang,
        boolean throughSmoke,
        boolean noScope,
        boolean killerBlind,
        boolean victimBlind,
        boolean killerBurning,
        boolean victimBurning,
        boolean killerLastAlive,
        Long grenadeId,
        String killerPlace,
        int firstDamageTick
) {
}
