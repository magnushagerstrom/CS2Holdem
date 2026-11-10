package main

import (
	"github.com/markus-wa/demoinfocs-golang/v5/pkg/demoinfocs/common"
)

// statsCollector adds up each player's numbers for one match. The server stores the raw sums
// and works out averages and percentages itself, so totals over many matches stay exact.
type statsCollector struct {
	players map[uint64]*ParsedPlayer
	// The last round each player was counted as playing, so a round is never counted twice
	lastRound map[uint64]int
}

func newStatsCollector() *statsCollector {
	return &statsCollector{players: map[uint64]*ParsedPlayer{}, lastRound: map[uint64]int{}}
}

// player returns the player's running totals, creating them the first time. Bots are skipped.
func (s *statsCollector) player(pl *common.Player) *ParsedPlayer {
	if pl == nil || pl.IsBot || pl.SteamID64 == 0 {
		return nil
	}
	stats, ok := s.players[pl.SteamID64]
	if !ok {
		stats = &ParsedPlayer{SteamID: pl.SteamID64}
		s.players[pl.SteamID64] = stats
	}
	if pl.Name != "" {
		stats.Name = pl.Name
	}
	return stats
}

// roundStarted counts a round for everyone on a team when the freeze time ends.
func (s *statsCollector) roundStarted(round int, playing []*common.Player) {
	for _, pl := range playing {
		if pl.Team != common.TeamTerrorists && pl.Team != common.TeamCounterTerrorists {
			continue // spectators
		}
		if stats := s.player(pl); stats != nil && s.lastRound[pl.SteamID64] != round {
			s.lastRound[pl.SteamID64] = round
			stats.RoundsPlayed++
		}
	}
}

// isGun is true for weapons that fire bullets: pistols, SMGs, heavy weapons and rifles.
func isGun(w *common.Equipment) bool {
	if w == nil {
		return false
	}
	switch w.Class() {
	case common.EqClassPistols, common.EqClassSMG, common.EqClassHeavy, common.EqClassRifle:
		return true
	}
	return false
}

func isUtility(w *common.Equipment) bool {
	return w != nil && (w.Type == common.EqHE || w.Type == common.EqMolotov || w.Type == common.EqIncendiary)
}

func (s *statsCollector) shot(shooter *common.Player, weapon *common.Equipment) {
	if stats := s.player(shooter); stats != nil && isGun(weapon) {
		stats.ShotsFired++
	}
}

// hurt counts damage to enemies only. Damage to teammates and to yourself never counts.
func (s *statsCollector) hurt(attacker, victim *common.Player, weapon *common.Equipment, damage int) {
	if attacker == nil || victim == nil || attacker.SteamID64 == victim.SteamID64 || attacker.Team == victim.Team {
		return
	}
	stats := s.player(attacker)
	if stats == nil {
		return
	}
	stats.Damage += damage
	if isUtility(weapon) {
		stats.UtilityDamage += damage
	}
	if isGun(weapon) {
		stats.ShotsHit++
	}
}

func (s *statsCollector) assist(assister, victim *common.Player, flash bool) {
	if assister == nil || victim == nil || assister.Team == victim.Team {
		return
	}
	if stats := s.player(assister); stats != nil {
		if flash {
			stats.FlashAssists++
		} else {
			stats.Assists++
		}
	}
}

func (s *statsCollector) mvp(pl *common.Player) {
	if stats := s.player(pl); stats != nil {
		stats.MVPs++
	}
}

// finish records which side each player ended the match on, so the server can tell who won.
func (s *statsCollector) finish(all []*common.Player) []ParsedPlayer {
	for _, pl := range all {
		if stats := s.player(pl); stats != nil {
			stats.Team = int(pl.Team)
		}
	}
	result := make([]ParsedPlayer, 0, len(s.players))
	for _, stats := range s.players {
		if stats.RoundsPlayed > 0 {
			result = append(result, *stats)
		}
	}
	return result
}
