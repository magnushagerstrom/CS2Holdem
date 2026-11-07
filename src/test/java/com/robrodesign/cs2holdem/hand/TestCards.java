package com.robrodesign.cs2holdem.hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Small factory methods that keep the hand tests short and readable. */
final class TestCards {

    static final long MATCH = 1001L;

    private TestCards() {
    }

    static Kill kill(WeaponGroup group, String area, Stamp... stamps) {
        return new Kill(group, 120, 100, area, Set.of(stamps));
    }

    static Kill rifle(Stamp... stamps) {
        return kill(WeaponGroup.RIFLE, "Mid", stamps);
    }

    static Event event(EventType type, int round, Stamp... killStamps) {
        return event(type, MATCH, round, killStamps);
    }

    static Event event(EventType type, long matchId, int round, Stamp... killStamps) {
        List<Kill> kills = new ArrayList<>();
        for (int i = 0; i < type.killCount(); i++) {
            kills.add(rifle(killStamps));
        }
        return new Event(type, matchId, round, kills);
    }

    static Event knife(int round) {
        return new Event(EventType.SPECIAL_KILL, MATCH, round,
                List.of(new Kill(WeaponGroup.MELEE, 200, 100, "Mid")));
    }

    static RoundKill roundKill(int round, WeaponGroup group, String area) {
        return new RoundKill(MATCH, round, kill(group, area));
    }
}
