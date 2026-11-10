package com.robrodesign.cs2holdem.detection;

import com.robrodesign.cs2holdem.hand.Event;
import com.robrodesign.cs2holdem.hand.EventType;
import com.robrodesign.cs2holdem.hand.Kill;
import com.robrodesign.cs2holdem.hand.RoundKill;
import com.robrodesign.cs2holdem.hand.Stamp;
import com.robrodesign.cs2holdem.hand.WeaponGroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Turns a parsed match into cards. The rules are in docs/CONCEPT.md, "Which kills count".
 *
 * Steps for every player:
 * 1. keep only kills that count (enemy, real players, live round, no exit frags, known weapon)
 * 2. give each kill its kill stamps
 * 3. group the kills by round and turn each round into at most one event
 * 4. give each event its event stamps
 */
public final class EventDetector {

    public static final int TICKS_PER_SECOND = 64;
    public static final int LIGHTNING_SECONDS = 5;
    public static final int MAX_EVENT_KILLS = 5;

    private EventDetector() {
    }

    /** Detects cards for every real player in the match. */
    public static List<DetectionResult> detectAll(ParsedMatch match) {
        Set<Long> players = new LinkedHashSet<>();
        for (ParsedKill kill : match.kills()) {
            if (!kill.killerIsBot() && kill.killerSteamId() != 0) {
                players.add(kill.killerSteamId());
            }
        }
        List<DetectionResult> results = new ArrayList<>();
        for (long player : players) {
            results.add(detect(match, player));
        }
        return results;
    }

    public static DetectionResult detect(ParsedMatch match, long steamId64) {
        Map<Integer, Integer> decidedTicks = new HashMap<>();
        for (ParsedRound round : match.rounds()) {
            decidedTicks.put(round.number(), round.decidedTick());
        }

        // Steps 1 and 2: counted kills, grouped by round in order
        Map<Integer, List<CountedKill>> byRound = new TreeMap<>();
        for (ParsedKill parsed : match.kills()) {
            if (parsed.killerSteamId() != steamId64) {
                continue;
            }
            Integer decidedTick = decidedTicks.get(parsed.round());
            toCountedKill(parsed, decidedTick).ifPresent(kill ->
                    byRound.computeIfAbsent(parsed.round(), r -> new ArrayList<>()).add(kill));
        }

        // Steps 3 and 4: one event per round at most
        List<Event> events = new ArrayList<>();
        List<RoundKill> roundKills = new ArrayList<>();
        for (Map.Entry<Integer, List<CountedKill>> entry : byRound.entrySet()) {
            int round = entry.getKey();
            List<CountedKill> kills = entry.getValue();
            kills.sort(Comparator.comparingInt(k -> k.parsed().tick()));
            for (CountedKill kill : kills) {
                roundKills.add(new RoundKill(match.matchId(), round, kill.parsed().tick(), kill.kill()));
            }
            toEvent(match.matchId(), round, kills).ifPresent(events::add);
        }
        return new DetectionResult(steamId64, events, roundKills);
    }

    // ---------------------------------------------------------------- step 1 and 2

    /** A kill that counts, together with the raw data the event stamps need. */
    record CountedKill(ParsedKill parsed, String weapon, Kill kill) {
    }

    static Optional<CountedKill> toCountedKill(ParsedKill parsed, Integer decidedTick) {
        if (!counts(parsed, decidedTick)) {
            return Optional.empty();
        }
        Optional<WeaponCatalog.WeaponInfo> weapon = WeaponCatalog.find(parsed.weapon());
        if (weapon.isEmpty()) {
            return Optional.empty(); // falling damage, the bomb or an unknown weapon
        }
        WeaponCatalog.WeaponInfo info = weapon.get();
        int damage = Math.max(0, Math.min(Kill.MAX_DAMAGE, parsed.damage()));
        String area = parsed.killerPlace() == null || parsed.killerPlace().isBlank()
                ? Kill.UNKNOWN_AREA : parsed.killerPlace();
        Kill kill = new Kill(info.group(), info.difficulty(), damage, area, killStamps(parsed, info));
        return Optional.of(new CountedKill(parsed, info.name(), kill));
    }

    static boolean counts(ParsedKill kill, Integer decidedTick) {
        if (kill.warmup() || decidedTick == null) {
            return false;
        }
        if (kill.killerIsBot() || kill.victimIsBot()) {
            return false;
        }
        if (kill.killerSteamId() == kill.victimSteamId() || kill.killerTeam() == kill.victimTeam()) {
            return false; // suicide or team kill
        }
        return kill.tick() <= decidedTick; // later kills are exit frags
    }

    static Set<Stamp> killStamps(ParsedKill kill, WeaponCatalog.WeaponInfo weapon) {
        Set<Stamp> stamps = EnumSet.noneOf(Stamp.class);
        if (kill.victimBlind()) {
            stamps.add(Stamp.FLASHED);
        }
        if (kill.throughSmoke()) {
            stamps.add(Stamp.SMOKED);
        }
        if (kill.victimBurning()) {
            stamps.add(Stamp.BURNING);
        }
        if (kill.killerBlind()) {
            stamps.add(Stamp.BLIND);
        }
        if (kill.killerBurning()) {
            stamps.add(Stamp.ON_FIRE);
        }
        if (kill.headshot()) {
            stamps.add(Stamp.HEADSHOT);
        }
        if (kill.wallbang()) {
            stamps.add(Stamp.WALLBANG);
        }
        // The demo flags every unzoomed kill; it only means something for a weapon that has a scope
        if (kill.noScope() && WeaponCatalog.hasScope(weapon.name())) {
            stamps.add(Stamp.NO_SCOPE);
        }
        return stamps;
    }

    // ---------------------------------------------------------------- step 3 and 4

    static Optional<Event> toEvent(long matchId, int round, List<CountedKill> kills) {
        int count = Math.min(kills.size(), MAX_EVENT_KILLS);
        List<CountedKill> used = kills.subList(0, count);

        EventType type = switch (count) {
            case 5 -> EventType.ACE;
            case 4 -> EventType.QUAD_KILL;
            case 3 -> EventType.TRIPLE_KILL;
            case 2 -> EventType.DOUBLE_KILL;
            case 1 -> Event.isSpecialKill(used.get(0).kill()) ? EventType.SPECIAL_KILL : null;
            default -> null;
        };
        if (type == null) {
            return Optional.empty(); // an ordinary single kill is not a card
        }

        List<Kill> eventKills = used.stream().map(CountedKill::kill).toList();
        return Optional.of(new Event(type, matchId, round, eventKills, eventStamps(used)));
    }

    static Set<Stamp> eventStamps(List<CountedKill> kills) {
        Set<Stamp> stamps = EnumSet.noneOf(Stamp.class);
        if (kills.stream().allMatch(k -> k.parsed().killerLastAlive())) {
            stamps.add(Stamp.CLUTCH);
        }
        if (kills.size() < 2) {
            return stamps;
        }

        ParsedKill first = kills.get(0).parsed();
        ParsedKill last = kills.get(kills.size() - 1).parsed();

        boolean sameGrenade = first.grenadeId() != null && kills.stream().allMatch(k ->
                WeaponCatalog.HE_GRENADE.equals(k.weapon())
                        && Objects.equals(k.parsed().grenadeId(), first.grenadeId()));
        if (sameGrenade) {
            stamps.add(Stamp.ONE_GRENADE);
        }

        String firstWeapon = kills.get(0).weapon();
        boolean sameBullet = kills.stream().allMatch(k ->
                k.parsed().tick() == first.tick()
                        && k.weapon().equals(firstWeapon)
                        && isBulletWeapon(k.kill().weaponGroup()));
        if (sameBullet) {
            stamps.add(Stamp.ONE_SHOT);
        }

        if (last.tick() - first.tick() <= LIGHTNING_SECONDS * TICKS_PER_SECOND) {
            stamps.add(Stamp.LIGHTNING);
        }
        return stamps;
    }

    private static boolean isBulletWeapon(WeaponGroup group) {
        return group != WeaponGroup.GRENADE && group != WeaponGroup.MELEE;
    }
}
