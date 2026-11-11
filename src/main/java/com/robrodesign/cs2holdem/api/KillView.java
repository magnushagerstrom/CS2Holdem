package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.hand.Kill;
import com.robrodesign.cs2holdem.hand.RoundKill;
import com.robrodesign.cs2holdem.hand.Stamp;

import java.util.List;
import java.util.Map;

/**
 * A kill as the API shows it. id, matchId and round are only set for ordinary kills that can be
 * picked one by one for a straight or a flush.
 */
public record KillView(String id, String matchId, String mapName, Integer round, Integer tick,
                       String weaponGroup, int damage, String mapArea, List<String> stamps, int points) {

    public static KillView from(Kill kill) {
        return new KillView(null, null, null, null, null, kill.weaponGroup().displayName(), kill.damage(),
                kill.mapArea(), stampNames(kill), kill.value());
    }

    public static KillView from(long id, RoundKill roundKill, Map<Long, String> maps) {
        Kill kill = roundKill.kill();
        return new KillView(String.valueOf(id), String.valueOf(roundKill.matchId()),
                maps.get(roundKill.matchId()), roundKill.round(),
                roundKill.tick(), kill.weaponGroup().displayName(), kill.damage(), kill.mapArea(),
                stampNames(kill), kill.value());
    }

    private static List<String> stampNames(Kill kill) {
        return kill.stamps().stream().map(Stamp::displayName).sorted().toList();
    }
}
