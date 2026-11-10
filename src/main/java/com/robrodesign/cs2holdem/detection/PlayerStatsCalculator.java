package com.robrodesign.cs2holdem.detection;

import com.robrodesign.cs2holdem.hand.WeaponGroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns a parsed match into one {@link PlayerMatchStats} per player.
 *
 * Pure Java, like the EventDetector: no database, so every rule can be unit tested.
 *
 * Which kills count here is wider than for cards. Statistics should match what players see on
 * the scoreboard, so exit frags count. Team kills, suicides, bots and warmup still do not.
 */
public final class PlayerStatsCalculator {

    private PlayerStatsCalculator() {
    }

    public static List<PlayerMatchStats> calculate(ParsedMatch match) {
        Map<Long, Counts> counts = new HashMap<>();
        List<ParsedKill> kills = match.kills().stream().filter(PlayerStatsCalculator::counts).toList();

        for (ParsedKill kill : kills) {
            Counts killer = counts.computeIfAbsent(kill.killerSteamId(), id -> new Counts());
            killer.kills++;
            if (kill.headshot()) {
                killer.headshotKills++;
            }
            if (kill.firstDamageTick() > 0 && kill.firstDamageTick() <= kill.tick() && isGunKill(kill)) {
                killer.ttkTicks += kill.tick() - kill.firstDamageTick();
                killer.ttkKills++;
            }
            counts.computeIfAbsent(kill.victimSteamId(), id -> new Counts()).deaths++;
        }

        // The opening kill is the first kill of each round
        Map<Integer, ParsedKill> firstByRound = new HashMap<>();
        for (ParsedKill kill : kills) {
            firstByRound.merge(kill.round(), kill, (a, b) -> b.tick() < a.tick() ? b : a);
        }
        for (ParsedKill opening : firstByRound.values()) {
            counts.computeIfAbsent(opening.killerSteamId(), id -> new Counts()).openingKills++;
            counts.computeIfAbsent(opening.victimSteamId(), id -> new Counts()).openingDeaths++;
        }

        List<PlayerMatchStats> result = new ArrayList<>();
        for (ParsedPlayer player : match.players()) {
            Counts c = counts.getOrDefault(player.steamId(), new Counts());
            result.add(new PlayerMatchStats(
                    player.steamId(), player.name(), team(player), result(player, match),
                    c.kills, c.deaths, player.assists(), player.flashAssists(), c.headshotKills,
                    player.damage(), player.utilityDamage(), player.roundsPlayed(), player.mvps(),
                    player.shotsFired(), player.shotsHit(), c.openingKills, c.openingDeaths,
                    c.ttkTicks, c.ttkKills));
        }
        result.sort(Comparator.comparingLong(PlayerMatchStats::steamId));
        return result;
    }

    static boolean counts(ParsedKill kill) {
        return !kill.warmup() && kill.round() > 0
                && !kill.killerIsBot() && !kill.victimIsBot()
                && kill.killerSteamId() != kill.victimSteamId()
                && kill.killerTeam() != kill.victimTeam();
    }

    /** Time to kill only makes sense for bullets: a grenade or a knife has no "first hit". */
    private static boolean isGunKill(ParsedKill kill) {
        Optional<WeaponCatalog.WeaponInfo> weapon = WeaponCatalog.find(kill.weapon());
        return weapon.isPresent()
                && weapon.get().group() != WeaponGroup.GRENADE
                && weapon.get().group() != WeaponGroup.MELEE;
    }

    /** team1Score belongs to the team that ended the match on CT, team2Score to the T side. */
    private static int team(ParsedPlayer player) {
        return player.team() == ParsedPlayer.COUNTER_TERRORIST ? 1 : 2;
    }

    private static PlayerMatchStats.MatchResult result(ParsedPlayer player, ParsedMatch match) {
        int own = player.team() == ParsedPlayer.COUNTER_TERRORIST ? match.team1Score() : match.team2Score();
        int other = player.team() == ParsedPlayer.COUNTER_TERRORIST ? match.team2Score() : match.team1Score();
        if (own > other) {
            return PlayerMatchStats.MatchResult.WIN;
        }
        return own < other ? PlayerMatchStats.MatchResult.LOSS : PlayerMatchStats.MatchResult.DRAW;
    }

    /** Running totals while counting. A plain mutable class: it never leaves this method. */
    private static final class Counts {
        int kills;
        int deaths;
        int headshotKills;
        int openingKills;
        int openingDeaths;
        long ttkTicks;
        int ttkKills;
    }
}
