package com.robrodesign.cs2holdem.detection;

import com.robrodesign.cs2holdem.hand.Event;
import com.robrodesign.cs2holdem.hand.EventType;
import com.robrodesign.cs2holdem.hand.Kill;
import com.robrodesign.cs2holdem.hand.Stamp;
import com.robrodesign.cs2holdem.hand.WeaponGroup;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.robrodesign.cs2holdem.detection.ParsedKillBuilder.PLAYER;
import static com.robrodesign.cs2holdem.detection.ParsedKillBuilder.kill;
import static org.assertj.core.api.Assertions.assertThat;

class EventDetectorTest {

    private static final long MATCH_ID = 3852342945172685115L;

    /** A match with 24 rounds, each decided at tick 10 000 + round x 10 000. */
    private static ParsedMatch match(ParsedKillBuilder... kills) {
        List<ParsedRound> rounds = new ArrayList<>();
        for (int round = 1; round <= 24; round++) {
            rounds.add(new ParsedRound(round, decidedTick(round)));
        }
        List<ParsedKill> parsed = new ArrayList<>();
        for (ParsedKillBuilder kill : kills) {
            parsed.add(kill.build());
        }
        return new ParsedMatch(MATCH_ID, "de_mirage", Instant.EPOCH, 2400, 13, 11, rounds, parsed);
    }

    private static int decidedTick(int round) {
        return 10_000 + round * 10_000;
    }

    private static int tickIn(int round, int offset) {
        return decidedTick(round) - 5_000 + offset;
    }

    private static List<Event> events(ParsedMatch match) {
        return EventDetector.detect(match, PLAYER).events();
    }

    // ---------- which kills count

    @Test
    void doubleKillInOneRoundBecomesAPair() {
        List<Event> events = events(match(
                kill().round(3).tick(tickIn(3, 0)),
                kill().round(3).tick(tickIn(3, 400))));

        assertThat(events).hasSize(1);
        assertThat(events.get(0).type()).isEqualTo(EventType.DOUBLE_KILL);
    }

    @Test
    void killsInDifferentRoundsAreNotCombined() {
        List<Event> events = events(match(
                kill().round(3).tick(tickIn(3, 0)),
                kill().round(4).tick(tickIn(4, 0))));

        assertThat(events).isEmpty();
    }

    @Test
    void ordinarySingleKillIsNotACardButCountsForStreaks() {
        DetectionResult result = EventDetector.detect(match(kill().round(5).tick(tickIn(5, 0))), PLAYER);

        assertThat(result.events()).isEmpty();
        assertThat(result.roundKills()).hasSize(1);
    }

    @Test
    void knifeKillIsASpecialKill() {
        List<Event> events = events(match(kill().round(2).tick(tickIn(2, 0)).weapon("knife_karambit")));

        assertThat(events.get(0).type()).isEqualTo(EventType.SPECIAL_KILL);
    }

    @Test
    void exitFragsDoNotCount() {
        List<Event> events = events(match(
                kill().round(6).tick(tickIn(6, 0)),
                kill().round(6).tick(decidedTick(6) + 100)));

        assertThat(events).isEmpty();
    }

    @Test
    void theKillThatDecidesTheRoundCounts() {
        List<Event> events = events(match(
                kill().round(6).tick(tickIn(6, 0)),
                kill().round(6).tick(decidedTick(6))));

        assertThat(events).hasSize(1);
    }

    @Test
    void teamKillsBotsAndWarmupDoNotCount() {
        DetectionResult result = EventDetector.detect(match(
                kill().round(7).tick(tickIn(7, 0)).victimTeam(2),
                kill().round(7).tick(tickIn(7, 10)).victimIsBot(),
                kill().round(7).tick(tickIn(7, 20)).warmup()), PLAYER);

        assertThat(result.roundKills()).isEmpty();
    }

    @Test
    void worldDamageDoesNotCount() {
        DetectionResult result = EventDetector.detect(match(kill().round(1).tick(tickIn(1, 0)).weapon("world")),
                PLAYER);

        assertThat(result.roundKills()).isEmpty();
    }

    @Test
    void onlyThePlayersOwnKillsAreUsed() {
        DetectionResult result = EventDetector.detect(match(
                kill().round(1).tick(tickIn(1, 0)).killer(ParsedKillBuilder.TEAMMATE)), PLAYER);

        assertThat(result.roundKills()).isEmpty();
    }

    @Test
    void damageIsCappedAt100() {
        DetectionResult result = EventDetector.detect(match(kill().round(1).tick(tickIn(1, 0)).damage(250)),
                PLAYER);

        assertThat(result.roundKills().get(0).kill().damage()).isEqualTo(100);
    }

    @Test
    void missingPlaceBecomesUnknownArea() {
        DetectionResult result = EventDetector.detect(match(kill().round(1).tick(tickIn(1, 0)).place(null)),
                PLAYER);

        assertThat(result.roundKills().get(0).kill().mapArea()).isEqualTo(Kill.UNKNOWN_AREA);
    }

    // ---------- kill stamps

    @Test
    void killStampsAreSetFromTheParsedFlags() {
        DetectionResult result = EventDetector.detect(match(kill().round(1).tick(tickIn(1, 0)).weapon("awp")
                .headshot().throughSmoke().noScope().killerBlind().victimBurning()), PLAYER);

        assertThat(result.roundKills().get(0).kill().stamps()).isEqualTo(java.util.Set.of(
                Stamp.HEADSHOT, Stamp.SMOKED, Stamp.NO_SCOPE, Stamp.BLIND, Stamp.BURNING));
    }

    @Test
    void noScopeOnlyCountsForSniperRifles() {
        DetectionResult result = EventDetector.detect(match(kill().round(1).tick(tickIn(1, 0)).noScope()), PLAYER);

        assertThat(result.roundKills().get(0).kill().stamps()).isEmpty();
    }

    // ---------- event stamps

    @Test
    void killsFromOneHeGrenadeGetOneGrenade() {
        Event event = events(match(
                kill().round(9).tick(tickIn(9, 0)).weapon("hegrenade").grenade(42),
                kill().round(9).tick(tickIn(9, 2)).weapon("hegrenade").grenade(42),
                kill().round(9).tick(tickIn(9, 3)).weapon("hegrenade").grenade(42))).get(0);

        assertThat(event.type()).isEqualTo(EventType.TRIPLE_KILL);
        assertThat(event.stamps().contains(Stamp.ONE_GRENADE)).isTrue();
    }

    @Test
    void killsFromTwoGrenadesDoNotGetOneGrenade() {
        Event event = events(match(
                kill().round(9).tick(tickIn(9, 0)).weapon("hegrenade").grenade(42),
                kill().round(9).tick(tickIn(9, 900)).weapon("hegrenade").grenade(43))).get(0);

        assertThat(event.stamps().contains(Stamp.ONE_GRENADE)).isFalse();
    }

    @Test
    void killsOnTheSameTickWithTheSameWeaponGetOneShot() {
        Event event = events(match(
                kill().round(10).tick(tickIn(10, 0)).weapon("awp"),
                kill().round(10).tick(tickIn(10, 0)).weapon("awp"))).get(0);

        assertThat(event.stamps().contains(Stamp.ONE_SHOT)).isTrue();
    }

    @Test
    void fastMultiKillGetsLightning() {
        Event fast = events(match(
                kill().round(11).tick(tickIn(11, 0)),
                kill().round(11).tick(tickIn(11, 64 * 4)))).get(0);
        Event slow = events(match(
                kill().round(11).tick(tickIn(11, 0)),
                kill().round(11).tick(tickIn(11, 64 * 6)))).get(0);

        assertThat(fast.stamps().contains(Stamp.LIGHTNING)).isTrue();
        assertThat(slow.stamps().contains(Stamp.LIGHTNING)).isFalse();
    }

    @Test
    void killsWhileLastAliveGetClutch() {
        Event event = events(match(
                kill().round(12).tick(tickIn(12, 0)).lastAlive(),
                kill().round(12).tick(tickIn(12, 1000)).lastAlive())).get(0);

        assertThat(event.stamps().contains(Stamp.CLUTCH)).isTrue();
    }

    // ---------- whole match

    @Test
    void fiveKillsInOneRoundBecomeAnAce() {
        List<Event> events = events(match(
                kill().round(13).tick(tickIn(13, 0)).headshot(),
                kill().round(13).tick(tickIn(13, 500)).headshot(),
                kill().round(13).tick(tickIn(13, 1000)).headshot(),
                kill().round(13).tick(tickIn(13, 1500)).headshot(),
                kill().round(13).tick(tickIn(13, 2000)).headshot()));

        assertThat(events.get(0).type()).isEqualTo(EventType.ACE);
        assertThat(events.get(0).isPerfectAce()).isTrue();
    }

    @Test
    void detectAllFindsEveryRealPlayer() {
        List<DetectionResult> results = EventDetector.detectAll(match(
                kill().round(1).tick(tickIn(1, 0)),
                kill().round(1).tick(tickIn(1, 10)).killer(ParsedKillBuilder.ENEMY)
                        .victim(ParsedKillBuilder.TEAMMATE).victimTeam(2),
                kill().round(2).tick(tickIn(2, 0)).killer(123L).killerIsBot()));

        assertThat(results).hasSize(2);
    }

    @Test
    void weaponCatalogKnowsKnifeSkins() {
        assertThat(WeaponCatalog.find("weapon_knife_butterfly").orElseThrow().group())
                .isEqualTo(WeaponGroup.MELEE);
        assertThat(WeaponCatalog.find("bayonet").orElseThrow().group()).isEqualTo(WeaponGroup.MELEE);
        assertThat(WeaponCatalog.find("world")).isEmpty();
    }
}
