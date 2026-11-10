package com.robrodesign.cs2holdem.detection;

/** Builds ParsedKill objects for tests, with sensible defaults so each test only sets what matters. */
final class ParsedKillBuilder {

    static final long PLAYER = 76561197960287930L;
    static final long ENEMY = 76561197960287931L;
    static final long TEAMMATE = 76561197960287932L;

    private int round = 1;
    private int tick = 1000;
    private boolean warmup;
    private long killer = PLAYER;
    private boolean killerIsBot;
    private int killerTeam = 2;
    private long victim = ENEMY;
    private boolean victimIsBot;
    private int victimTeam = 3;
    private String weapon = "ak47";
    private int damage = 100;
    private boolean headshot;
    private boolean wallbang;
    private boolean throughSmoke;
    private boolean noScope;
    private boolean killerBlind;
    private boolean victimBlind;
    private boolean killerBurning;
    private boolean victimBurning;
    private boolean killerLastAlive;
    private Long grenadeId;
    private String place = "Mid";
    private int firstDamageTick;

    static ParsedKillBuilder kill() {
        return new ParsedKillBuilder();
    }

    ParsedKillBuilder round(int value) { round = value; return this; }
    ParsedKillBuilder tick(int value) { tick = value; return this; }
    ParsedKillBuilder warmup() { warmup = true; return this; }
    ParsedKillBuilder killer(long value) { killer = value; return this; }
    ParsedKillBuilder killerIsBot() { killerIsBot = true; return this; }
    ParsedKillBuilder victim(long value) { victim = value; return this; }
    ParsedKillBuilder victimIsBot() { victimIsBot = true; return this; }
    ParsedKillBuilder victimTeam(int value) { victimTeam = value; return this; }
    ParsedKillBuilder weapon(String value) { weapon = value; return this; }
    ParsedKillBuilder damage(int value) { damage = value; return this; }
    ParsedKillBuilder headshot() { headshot = true; return this; }
    ParsedKillBuilder wallbang() { wallbang = true; return this; }
    ParsedKillBuilder throughSmoke() { throughSmoke = true; return this; }
    ParsedKillBuilder noScope() { noScope = true; return this; }
    ParsedKillBuilder killerBlind() { killerBlind = true; return this; }
    ParsedKillBuilder victimBlind() { victimBlind = true; return this; }
    ParsedKillBuilder killerBurning() { killerBurning = true; return this; }
    ParsedKillBuilder victimBurning() { victimBurning = true; return this; }
    ParsedKillBuilder lastAlive() { killerLastAlive = true; return this; }
    ParsedKillBuilder grenade(long id) { grenadeId = id; return this; }
    ParsedKillBuilder place(String value) { place = value; return this; }
    ParsedKillBuilder firstDamage(int value) { firstDamageTick = value; return this; }
    ParsedKillBuilder killer(long steamId, int team) { killer = steamId; killerTeam = team; return this; }
    ParsedKillBuilder victim(long steamId, int team) { victim = steamId; victimTeam = team; return this; }

    ParsedKill build() {
        return new ParsedKill(round, tick, warmup, killer, killerIsBot, killerTeam, victim, victimIsBot,
                victimTeam, weapon, damage, headshot, wallbang, throughSmoke, noScope, killerBlind,
                victimBlind, killerBurning, victimBurning, killerLastAlive, grenadeId, place, firstDamageTick);
    }
}
