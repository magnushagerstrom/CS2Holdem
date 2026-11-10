package main

import "time"

// These structs are turned into the JSON the Java records ParsedMatch, ParsedRound
// and ParsedKill expect. The json tags must match the Java field names exactly.

type ParsedMatch struct {
	MatchID         int64         `json:"matchId"`
	MapName         string        `json:"mapName"`
	PlayedAt        *time.Time    `json:"playedAt"` // nil: the server uses the time it found the match
	DurationSeconds int           `json:"durationSeconds"`
	Team1Score      int           `json:"team1Score"`
	Team2Score      int           `json:"team2Score"`
	Rounds          []ParsedRound `json:"rounds"`
	Kills           []ParsedKill  `json:"kills"`
	Players         []ParsedPlayer `json:"players"`
}

// ParsedPlayer is one player's totals for the whole match (live rounds only, no warmup).
// Team is the side the player ended the match on: 2 = T, 3 = CT.
type ParsedPlayer struct {
	SteamID       uint64 `json:"steamId"`
	Name          string `json:"name"`
	Team          int    `json:"team"`
	RoundsPlayed  int    `json:"roundsPlayed"`
	Damage        int    `json:"damage"`
	UtilityDamage int    `json:"utilityDamage"`
	Assists       int    `json:"assists"`
	FlashAssists  int    `json:"flashAssists"`
	MVPs          int    `json:"mvps"`
	ShotsFired    int    `json:"shotsFired"`
	ShotsHit      int    `json:"shotsHit"`
}

type ParsedRound struct {
	Number      int `json:"number"`
	DecidedTick int `json:"decidedTick"`
}

type ParsedKill struct {
	Round           int    `json:"round"`
	Tick            int    `json:"tick"`
	Warmup          bool   `json:"warmup"`
	KillerSteamID   uint64 `json:"killerSteamId"`
	KillerIsBot     bool   `json:"killerIsBot"`
	KillerTeam      int    `json:"killerTeam"`
	VictimSteamID   uint64 `json:"victimSteamId"`
	VictimIsBot     bool   `json:"victimIsBot"`
	VictimTeam      int    `json:"victimTeam"`
	Weapon          string `json:"weapon"`
	Damage          int    `json:"damage"`
	Headshot        bool   `json:"headshot"`
	Wallbang        bool   `json:"wallbang"`
	ThroughSmoke    bool   `json:"throughSmoke"`
	NoScope         bool   `json:"noScope"`
	KillerBlind     bool   `json:"killerBlind"`
	VictimBlind     bool   `json:"victimBlind"`
	KillerBurning   bool   `json:"killerBurning"`
	VictimBurning   bool   `json:"victimBurning"`
	KillerLastAlive bool   `json:"killerLastAlive"`
	GrenadeID       *int64 `json:"grenadeId"`
	KillerPlace     string `json:"killerPlace"`
	// FirstDamageTick is when the killer first hurt this victim in the round (0 = unknown).
	// The server uses it for time to kill.
	FirstDamageTick int `json:"firstDamageTick"`
}
