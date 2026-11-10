package com.robrodesign.cs2holdem.detection;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static com.robrodesign.cs2holdem.detection.ParsedKillBuilder.ENEMY;
import static com.robrodesign.cs2holdem.detection.ParsedKillBuilder.PLAYER;
import static com.robrodesign.cs2holdem.detection.ParsedKillBuilder.TEAMMATE;
import static com.robrodesign.cs2holdem.detection.ParsedKillBuilder.kill;
import static org.assertj.core.api.Assertions.assertThat;

class PlayerStatsCalculatorTest {

    private static final int T = ParsedPlayer.TERRORIST;
    private static final int CT = ParsedPlayer.COUNTER_TERRORIST;

    private static ParsedPlayer player(long steamId, int team) {
        return new ParsedPlayer(steamId, "name", team, 24, 2000, 150, 3, 1, 2, 400, 120);
    }

    /** PLAYER is a T, ENEMY a CT. The CT side won 13-11 unless the test says otherwise. */
    private static ParsedMatch match(int ctScore, int tScore, ParsedKillBuilder... kills) {
        List<ParsedKill> parsed = java.util.Arrays.stream(kills).map(ParsedKillBuilder::build).toList();
        return new ParsedMatch(1L, "de_mirage", Instant.EPOCH, 2400, ctScore, tScore, List.of(), parsed,
                List.of(player(PLAYER, T), player(ENEMY, CT)));
    }

    private static PlayerMatchStats stats(ParsedMatch match, long steamId) {
        return PlayerStatsCalculator.calculate(match).stream()
                .filter(s -> s.steamId() == steamId).findFirst().orElseThrow();
    }

    @Test
    void killsDeathsAndHeadshotsAreCounted() {
        ParsedMatch match = match(13, 11,
                kill().round(1).tick(100).headshot(),
                kill().round(2).tick(200),
                kill().round(3).tick(300).killer(ENEMY, CT).victim(PLAYER, T));

        PlayerMatchStats player = stats(match, PLAYER);

        assertThat(player.kills()).isEqualTo(2);
        assertThat(player.headshotKills()).isEqualTo(1);
        assertThat(player.deaths()).isEqualTo(1);
    }

    @Test
    void teamKillsBotsAndWarmupDoNotCount() {
        ParsedMatch match = match(13, 11,
                kill().round(1).tick(100).victim(TEAMMATE, T),
                kill().round(1).tick(110).victimIsBot(),
                kill().round(1).tick(120).warmup());

        assertThat(stats(match, PLAYER).kills()).isZero();
    }

    @Test
    void theFirstKillOfARoundIsTheOpeningKill() {
        ParsedMatch match = match(13, 11,
                kill().round(4).tick(500),
                kill().round(4).tick(300).killer(ENEMY, CT).victim(PLAYER, T));

        assertThat(stats(match, ENEMY).openingKills()).isEqualTo(1);
        assertThat(stats(match, PLAYER).openingDeaths()).isEqualTo(1);
        assertThat(stats(match, PLAYER).openingKills()).isZero();
    }

    @Test
    void timeToKillIsFromTheFirstHitToTheKill() {
        ParsedMatch match = match(13, 11,
                kill().round(1).tick(1000).firstDamage(968),  // half a second at 64 ticks
                kill().round(2).tick(2000).firstDamage(2000)); // a one-tap

        PlayerMatchStats player = stats(match, PLAYER);

        assertThat(player.ttkKills()).isEqualTo(2);
        assertThat(player.ttkTicks()).isEqualTo(32L);
    }

    @Test
    void grenadeKillsHaveNoTimeToKill() {
        ParsedMatch match = match(13, 11, kill().round(1).tick(1000).firstDamage(990).weapon("hegrenade"));

        assertThat(stats(match, PLAYER).ttkKills()).isZero();
    }

    @Test
    void theTeamThatEndedOnCtWonWhenTeam1HasMoreRounds() {
        ParsedMatch match = match(13, 11);

        assertThat(stats(match, ENEMY).result()).isEqualTo(PlayerMatchStats.MatchResult.WIN);
        assertThat(stats(match, PLAYER).result()).isEqualTo(PlayerMatchStats.MatchResult.LOSS);
        assertThat(stats(match, ENEMY).team()).isEqualTo(1);
    }

    @Test
    void anEqualScoreIsADraw() {
        assertThat(stats(match(12, 12), PLAYER).result()).isEqualTo(PlayerMatchStats.MatchResult.DRAW);
    }

    @Test
    void theWorkersTotalsArePassedOn() {
        PlayerMatchStats player = stats(match(13, 11), PLAYER);

        assertThat(player.damage()).isEqualTo(2000);
        assertThat(player.roundsPlayed()).isEqualTo(24);
        assertThat(player.shotsHit()).isEqualTo(120);
    }
}
