package com.robrodesign.cs2holdem.locator;

import in.dragonbra.javasteam.protobufs.cs.Cstrike15Gcmessages.CDataGCCStrike15_v2_MatchInfo;
import in.dragonbra.javasteam.protobufs.cs.Cstrike15Gcmessages.CMsgGCCStrike15_v2_MatchmakingServerRoundStats;

import java.util.Optional;

/**
 * Finds the demo's download link in the Game Coordinator's answer.
 *
 * Valve does not have a field called "demo url". The link hides in the field "map" of the
 * statistics for the LAST round of the match (a leftover from CS:GO that Valve never renamed).
 * Earlier rounds have no link. When Valve has deleted the demo, no round has one.
 *
 * Kept apart from the Steam connection so the rule can be unit tested without logging in.
 */
final class DemoUrls {

    private DemoUrls() {
    }

    static Optional<String> from(CDataGCCStrike15_v2_MatchInfo match) {
        // Walk backwards: the link is normally in the last round, so we usually stop at once.
        for (int i = match.getRoundstatsallCount() - 1; i >= 0; i--) {
            Optional<String> url = urlOf(match.getRoundstatsall(i));
            if (url.isPresent()) {
                return url;
            }
        }
        // Very old matches only have the older, single "legacy" statistics field.
        if (match.hasRoundstatsLegacy()) {
            return urlOf(match.getRoundstatsLegacy());
        }
        return Optional.empty();
    }

    private static Optional<String> urlOf(CMsgGCCStrike15_v2_MatchmakingServerRoundStats round) {
        if (round.hasMap() && round.getMap().startsWith("http")) {
            return Optional.of(round.getMap());
        }
        return Optional.empty();
    }
}
