package com.robrodesign.cs2holdem.match;

import com.robrodesign.cs2holdem.detection.PlayerMatchStats;
import com.robrodesign.cs2holdem.player.Player;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One player's performance in one match, as raw sums (see V1 and V5).
 * Averages and percentages are worked out when shown, so totals over many matches stay exact.
 */
@Entity
@Table(name = "match_player_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchPlayerStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: matchen hämtas från databasen först när någon faktiskt anropar getMatch()
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "steam_id64", nullable = false)
    private Player player;

    @Column(name = "team", nullable = false)
    private int team; // 1 eller 2, se team1_score/team2_score i matches

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 4)
    private PlayerMatchStats.MatchResult result;

    @Column(name = "kills", nullable = false)
    private int kills;

    @Column(name = "deaths", nullable = false)
    private int deaths;

    @Column(name = "assists", nullable = false)
    private int assists;

    @Column(name = "flash_assists", nullable = false)
    private int flashAssists;

    @Column(name = "headshot_kills", nullable = false)
    private int headshotKills;

    @Column(name = "damage_total", nullable = false)
    private int damageTotal;

    @Column(name = "utility_damage", nullable = false)
    private int utilityDamage;

    @Column(name = "rounds_played", nullable = false)
    private int roundsPlayed;

    @Column(name = "mvps", nullable = false)
    private int mvps;

    @Column(name = "shots_fired", nullable = false)
    private int shotsFired;

    @Column(name = "shots_hit", nullable = false)
    private int shotsHit;

    @Column(name = "opening_kills", nullable = false)
    private int openingKills;

    @Column(name = "opening_deaths", nullable = false)
    private int openingDeaths;

    @Column(name = "ttk_ticks", nullable = false)
    private long ttkTicks;

    @Column(name = "ttk_kills", nullable = false)
    private int ttkKills;

    public MatchPlayerStats(Match match, Player player, PlayerMatchStats stats) {
        this.match = match;
        this.player = player;
        this.team = stats.team();
        this.result = stats.result();
        this.kills = stats.kills();
        this.deaths = stats.deaths();
        this.assists = stats.assists();
        this.flashAssists = stats.flashAssists();
        this.headshotKills = stats.headshotKills();
        this.damageTotal = stats.damage();
        this.utilityDamage = stats.utilityDamage();
        this.roundsPlayed = stats.roundsPlayed();
        this.mvps = stats.mvps();
        this.shotsFired = stats.shotsFired();
        this.shotsHit = stats.shotsHit();
        this.openingKills = stats.openingKills();
        this.openingDeaths = stats.openingDeaths();
        this.ttkTicks = stats.ttkTicks();
        this.ttkKills = stats.ttkKills();
    }
}
