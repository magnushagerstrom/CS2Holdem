package main

import (
	"errors"
	"fmt"
	"io"

	dem "github.com/markus-wa/demoinfocs-golang/v5/pkg/demoinfocs"
	"github.com/markus-wa/demoinfocs-golang/v5/pkg/demoinfocs/common"
	"github.com/markus-wa/demoinfocs-golang/v5/pkg/demoinfocs/events"
)

const (
	ticksPerSecond = 64
	// A player hurt by fire within this many ticks counts as burning
	burnWindow = ticksPerSecond
	// An HE kill within this many ticks of the killer's grenade exploding belongs to that grenade
	grenadeWindow = ticksPerSecond
)

type pairKey struct {
	round          int
	killer, victim uint64
}

type grenadeBlast struct {
	entityID int
	tick     int
}

// ParseDemo reads a whole demo and collects what the Java EventDetector needs.
// It reports raw facts only; deciding what counts (bots, warmup, exit frags ...) is the server's job.
func ParseDemo(matchID int64, demo io.Reader) (*ParsedMatch, error) {
	p := dem.NewParser(demo)
	defer p.Close()

	result := &ParsedMatch{MatchID: matchID}
	round := 0
	damage := map[pairKey]int{}         // health removed per killer and victim, per round
	lastFireHurt := map[uint64]int{}    // steamID -> tick the player last took fire damage
	lastHE := map[uint64]grenadeBlast{} // steamID -> the player's last HE grenade explosion

	p.RegisterEventHandler(func(events.RoundStart) {
		gs := p.GameState()
		if !gs.IsWarmupPeriod() {
			round = gs.TotalRoundsPlayed() + 1
		}
	})

	p.RegisterEventHandler(func(e events.RoundEnd) {
		if round > 0 && !p.GameState().IsWarmupPeriod() {
			result.Rounds = append(result.Rounds, ParsedRound{Number: round, DecidedTick: p.GameState().IngameTick()})
		}
	})

	p.RegisterEventHandler(func(e events.PlayerHurt) {
		if e.Player == nil {
			return
		}
		if e.Weapon != nil && (e.Weapon.Type == common.EqMolotov || e.Weapon.Type == common.EqIncendiary) {
			lastFireHurt[e.Player.SteamID64] = p.GameState().IngameTick()
		}
		if e.Attacker != nil {
			damage[pairKey{round, e.Attacker.SteamID64, e.Player.SteamID64}] += e.HealthDamageTaken
		}
	})

	p.RegisterEventHandler(func(e events.HeExplode) {
		if e.Thrower != nil {
			lastHE[e.Thrower.SteamID64] = grenadeBlast{e.GrenadeEntityID, p.GameState().IngameTick()}
		}
	})

	p.RegisterEventHandler(func(e events.Kill) {
		if e.Killer == nil || e.Victim == nil || e.Weapon == nil {
			return // suicide, fall damage, the bomb ...
		}
		gs := p.GameState()
		tick := gs.IngameTick()
		burning := func(pl *common.Player) bool {
			t, ok := lastFireHurt[pl.SteamID64]
			return ok && tick-t <= burnWindow
		}

		kill := ParsedKill{
			Round:           round,
			Tick:            tick,
			Warmup:          gs.IsWarmupPeriod(),
			KillerSteamID:   e.Killer.SteamID64,
			KillerIsBot:     e.Killer.IsBot,
			KillerTeam:      int(e.Killer.Team),
			VictimSteamID:   e.Victim.SteamID64,
			VictimIsBot:     e.Victim.IsBot,
			VictimTeam:      int(e.Victim.Team),
			Weapon:          weaponName(e.Weapon),
			Damage:          damage[pairKey{round, e.Killer.SteamID64, e.Victim.SteamID64}],
			Headshot:        e.IsHeadshot,
			Wallbang:        e.PenetratedObjects > 0,
			ThroughSmoke:    e.ThroughSmoke,
			NoScope:         e.NoScope,
			KillerBlind:     e.AttackerBlind,
			VictimBlind:     e.Victim.IsBlinded(),
			KillerBurning:   burning(e.Killer),
			VictimBurning:   burning(e.Victim),
			KillerLastAlive: aliveTeammates(gs.Team(e.Killer.Team), e.Killer) == 0,
			KillerPlace:     e.Killer.LastPlaceName(),
		}
		if e.Weapon.Type == common.EqHE {
			if blast, ok := lastHE[e.Killer.SteamID64]; ok && tick-blast.tick <= grenadeWindow {
				id := int64(blast.entityID)
				kill.GrenadeID = &id
			}
		}
		result.Kills = append(result.Kills, kill)
	})

	if err := p.ParseToEnd(); err != nil && !errors.Is(err, dem.ErrUnexpectedEndOfDemo) {
		return nil, fmt.Errorf("could not read the demo: %w", err)
	}

	gs := p.GameState()
	result.MapName = p.Header().MapName
	result.DurationSeconds = int(p.CurrentTime().Seconds())
	result.Team1Score = gs.TeamCounterTerrorists().Score()
	result.Team2Score = gs.TeamTerrorists().Score()
	return result, nil
}

// weaponName gives the weapon's display name, e.g. "AK-47". The server's WeaponCatalog
// understands both these names and Valve's internal ones.
func weaponName(w *common.Equipment) string {
	if w.Type == common.EqKnife {
		return "knife"
	}
	return w.String()
}

// aliveTeammates counts the killer's living teammates, not counting the killer.
func aliveTeammates(team *common.TeamState, killer *common.Player) int {
	if team == nil {
		return 0
	}
	count := 0
	for _, member := range team.Members() {
		if member != nil && member.SteamID64 != killer.SteamID64 && member.IsAlive() {
			count++
		}
	}
	return count
}
