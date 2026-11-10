package com.robrodesign.cs2holdem.locator;

import in.dragonbra.javasteam.protobufs.cs.Cstrike15Gcmessages.CDataGCCStrike15_v2_MatchInfo;
import in.dragonbra.javasteam.protobufs.cs.Cstrike15Gcmessages.CMsgGCCStrike15_v2_MatchmakingServerRoundStats;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemoUrlsTest {

    private static final String DEMO = "http://replay183.valve.net/730/003852342945172685115_1367150024.dem.bz2";

    private static CMsgGCCStrike15_v2_MatchmakingServerRoundStats round() {
        return CMsgGCCStrike15_v2_MatchmakingServerRoundStats.newBuilder().build();
    }

    private static CMsgGCCStrike15_v2_MatchmakingServerRoundStats lastRound(String map) {
        return CMsgGCCStrike15_v2_MatchmakingServerRoundStats.newBuilder().setMap(map).build();
    }

    @Test
    void theLinkIsInTheLastRound() {
        var match = CDataGCCStrike15_v2_MatchInfo.newBuilder()
                .addRoundstatsall(round())
                .addRoundstatsall(round())
                .addRoundstatsall(lastRound(DEMO))
                .build();

        assertThat(DemoUrls.from(match)).contains(DEMO);
    }

    @Test
    void noLinkWhenValveHasDeletedTheDemo() {
        var match = CDataGCCStrike15_v2_MatchInfo.newBuilder()
                .addRoundstatsall(round())
                .addRoundstatsall(round())
                .build();

        assertThat(DemoUrls.from(match)).isEmpty();
    }

    @Test
    void ignoresAMapFieldThatIsNotALink() {
        var match = CDataGCCStrike15_v2_MatchInfo.newBuilder()
                .addRoundstatsall(lastRound("de_mirage"))
                .build();

        assertThat(DemoUrls.from(match)).isEmpty();
    }

    @Test
    void oldMatchesHaveTheLinkInTheLegacyField() {
        var match = CDataGCCStrike15_v2_MatchInfo.newBuilder()
                .setRoundstatsLegacy(lastRound(DEMO))
                .build();

        assertThat(DemoUrls.from(match)).contains(DEMO);
    }

    @Test
    void aMatchWithoutRoundsHasNoLink() {
        assertThat(DemoUrls.from(CDataGCCStrike15_v2_MatchInfo.getDefaultInstance())).isEmpty();
    }
}
