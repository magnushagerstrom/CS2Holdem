package com.robrodesign.cs2holdem.match;

import com.robrodesign.cs2holdem.player.Player;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** En spelares prestation i en match. */
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

    @Column(name = "kills", nullable = false)
    private int kills;

    @Column(name = "deaths", nullable = false)
    private int deaths;

    @Column(name = "assists", nullable = false)
    private int assists;

    @Column(name = "headshot_kills", nullable = false)
    private int headshotKills;

    @Column(name = "damage_total", nullable = false)
    private int damageTotal;

    @Column(name = "rounds_played", nullable = false)
    private int roundsPlayed;

    @Column(name = "mvps", nullable = false)
    private int mvps;

    public MatchPlayerStats(Match match, Player player, int team, int kills, int deaths, int assists,
                            int headshotKills, int damageTotal, int roundsPlayed, int mvps) {
        this.match = match;
        this.player = player;
        this.team = team;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.headshotKills = headshotKills;
        this.damageTotal = damageTotal;
        this.roundsPlayed = roundsPlayed;
        this.mvps = mvps;
    }

    /** Average Damage per Round. Räknas fram, sparas inte. */
    public double calculateAdr() {
        return roundsPlayed == 0 ? 0.0 : (double) damageTotal / roundsPlayed;
    }

    /** Andel kills som var headshots, i procent. */
    public double calculateHeadshotPercentage() {
        return kills == 0 ? 0.0 : 100.0 * headshotKills / kills;
    }
}
