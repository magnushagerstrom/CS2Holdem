# CS2Holdem – Game Concept

> Collect the best moments from your Counter-Strike 2 season and play them as a poker hand.

This document describes the rules of CS2Holdem. It is the source of truth for the hand
evaluation engine and the data model. Rules marked **(proposal)** are not final yet.

---

## 1. The idea in short

1. While you play CS2, CS2Holdem collects your **events**: the highlight moments of a round,
   such as a triple kill, an ace or a knife kill.
2. Every event is a **card** in your collection.
3. From your cards you build the **strongest poker hand** you can.
4. The game follows the **CS2 seasons**. When a season ends, the player with the strongest
   hand wins, and the glory lies in how spectacular the events behind that hand were.

## 2. Events (the cards)

An event always happens **within a single round**. Ordinary single kills do not create cards;
CS2Holdem only cares about moments worth showing off.

| Event        | What happened in the round                     | Kills |
|--------------|------------------------------------------------|-------|
| Double kill  | 2 kills in the round                           | 2     |
| Triple kill  | 3 kills in the round                           | 3     |
| Quad kill    | 4 kills in the round                           | 4     |
| Ace          | 5 kills in the round (the whole enemy team)    | 5     |
| Special kill | A single kill that is remarkable in itself, e.g. a knife or Zeus kill | 1 |

A round produces **at most one event per player**. A 5-kill round is an Ace; it cannot be
split into a triple kill and a double kill.

### Which kills count

All kills in an event must happen **in the same round**. On top of that, a kill only counts if:

- the victim was an **enemy** (no team kills),
- the victim was a **real player**, not a bot,
- it happened during a **live round**, not during warmup and not after the round was decided,
- it was dealt by the player, not by falling damage or other world damage,
- the match was played on **Valve's official matchmaking** (Premier, Competitive, Wingman).

**No exit frags:** kills made after the round has already been decided, e.g. after the bomb
exploded, was defused or the last enemy died, do **not** count. Only kills that happen while
the round is still being played can be part of an event. Exit frags are often used to pad
statistics and would make events too easy to farm.

**Speed (proposal):** there is no time limit for an event to count; the round is the window,
just like in CS2 itself. Fast multi-kills are instead rewarded with a stamp (see *Lightning*
below).

## 3. Hands

A hand is built from your events and contains **at most five kills**, just like five cards.
Events in a hand always come from **different rounds**, but they may come from **any match
during the same season**.

Every poker hand has a CS2Holdem equivalent:

| #  | Hand (weakest → strongest) | Built from | Example |
|----|-----------------|----------------------------------------------------------|--------------------------------------|
| 1  | High card       | one special kill                                         | a knife kill                         |
| 2  | Pair            | one double kill                                          | 2k                                   |
| 3  | Two pair        | two double kills                                         | 2k + 2k                              |
| 4  | Three of a kind | one triple kill                                          | 3k                                   |
| 5  | Straight        | 5 kills in **5 connected areas** of the same map         | T Ramp → Palace → A Site → Jungle → Connector on de_mirage |
| 6  | Flush           | 5 kills that all share the **same suit**                 | five headshots in Mid on de_mirage   |
| 7  | Full house      | one triple kill + one double kill                        | 3k + 2k                              |
| 8  | Four of a kind  | one quad kill                                            | 4k                                   |
| 9  | Straight flush  | a straight where all five kills share the same stamp     | walking through Mirage with five headshots |
| 10 | Five of a kind  | one ace                                                  | 5k                                   |
| 11 | **Royal flush** | one ace that is **all headshots** *or* has **500 total damage** | the perfect ace              |

Five of a kind does not exist in standard poker, but it is a top hand in games played with
wild cards. That makes it a fitting reward for an ace.

**Royal flush** is the strongest hand in CS2Holdem: a perfect ace. An ace qualifies if either

- **all five kills were headshots**, or
- the player dealt **at least 500 total damage** to the five enemies.

Damage is counted as health actually removed, so it is capped at 100 per enemy per round.
500 damage is therefore the maximum possible, and it is only reached when the player dealt
**every point of damage** to all five enemies alone. In other words: a completely clean solo ace.

### Rules for building hands

- A hand contains **at most five kills**. Straight, flush and straight flush need **exactly five**.
- **Special kills** (single kills such as a knife or Zeus kill) are real cards. They form a high
  card on their own and can fill a hand up to five kills.
- **The highest hand always wins.** If the same cards qualify for several hands, the engine
  classifies them as the highest one. A 3k + 2k where all five kills are headshots is both a
  full house and a flush; it counts as a **full house**. Straights and flushes are how weaker
  combinations, such as two pair plus a knife kill, can be upgraded.

### Straights: walking through the map

A straight means **moving through the map with your kills**. The five kills must happen in
**five different areas** of the **same map**, and the areas must form a **connected chain**:
each area borders the next.

Example on *de_mirage*: kills in *T Ramp*, *Palace*, *A Site*, *Jungle* and *Connector*.

- The kills may come from **different rounds and matches**, as long as they are all on the same map.
- The **order** in which the kills happened does not matter, just like in poker, where
  5-6-7-8-9 is a straight no matter in which order the cards were dealt.
- Areas use the callout names shown in the game. Which areas border each other is stored per
  map, either entered by hand or derived from the map's navigation mesh (to be decided).

### Weapon groups

Weapons are grouped for scoring and for the *Arsenal* stamp. An AK-47 and an M4A4 are the
same group.

| Group    | Weapons                                                        |
|----------|----------------------------------------------------------------|
| Rifle    | AK-47, M4A4, M4A1-S, FAMAS, Galil AR, AUG, SG 553              |
| Sniper   | AWP, SSG 08, SCAR-20, G3SG1                                    |
| Pistol   | Glock-18, USP-S, P2000, P250, Five-SeveN, Tec-9, CZ75-Auto, Desert Eagle, R8 Revolver, Dual Berettas |
| SMG      | MAC-10, MP9, MP7, MP5-SD, UMP-45, P90, PP-Bizon                |
| Shotgun  | Nova, XM1014, Sawed-Off, MAG-7                                 |
| Heavy    | M249, Negev                                                    |
| Grenade  | HE grenade, Molotov, Incendiary                                |
| Melee    | Knife, Zeus x27                                                |

### Suits

A **suit** is what the five kills of a flush have in common. There are two kinds:

- **Stamp suit:** all five kills carry the same kill stamp, e.g. five *Smoked* kills.
- **Location suit:** all five kills happened in the same **area on the same map**, using the
  callout names shown in the game, e.g. *Mid* on *de_mirage*.

The best flushes combine both: **five headshots in Mid on de_mirage** shares a stamp *and* a
location, and earns the points for both.

## 4. Stamps

Stamps describe **how** the kills happened. They are the cherry on top: every stamp adds
points, and they are what make one full house more impressive than another.

There are three kinds of stamps: on a kill, on an event and on a whole hand.

### Kill stamps

A kill stamp belongs to **one kill**. Every kill in an event can carry its own stamps, and a
single kill can carry several. A full house where two kills went through a smoke earns the
*Smoked* points twice.

**The enemy was impaired:**

| Stamp           | When it is given                                        |
|-----------------|---------------------------------------------------------|
| **Flashed**     | the enemy was blinded by a flashbang                    |
| **Smoked**      | the enemy was killed through or inside a smoke          |
| **Burning**     | the enemy was standing in fire (molotov or incendiary)  |

**The player was impaired** (harder, so worth more):

| Stamp           | When it is given                                        |
|-----------------|---------------------------------------------------------|
| **Blind**       | the player was blinded by a flashbang                   |
| **On fire**     | the player was standing in fire                         |

**Skill shots:**

| Stamp           | When it is given                                        |
|-----------------|---------------------------------------------------------|
| **Headshot**    | the kill was a headshot                                 |
| **Wallbang**    | the bullet went through a wall or other object          |
| **No-scope**    | a sniper kill without using the scope                   |

### Event stamps

An event stamp belongs to the **whole event**.

| Stamp           | When it is given                                              |
|-----------------|---------------------------------------------------------------|
| **One grenade** | all kills in the event came from the same HE grenade          |
| **One shot**    | all kills in the event came from a single bullet (collateral) |
| **Clutch**      | the event happened while the player was the last one alive    |
| **Lightning** (proposal) | all kills in the event happened within a few seconds |

### Hand stamps

A hand stamp belongs to the **whole hand**.

| Stamp           | When it is given                                              |
|-----------------|---------------------------------------------------------------|
| **Arsenal**     | the five kills in the hand were made with five different weapon groups |

Example: a full house where one HE grenade kills three (*One grenade*) and one AWP shot kills
two (*One shot*), and one of the AWP victims was flashed (*Flashed*), collects three stamps.

### Starting points (proposal)

Every stamp has a point value. Rarer and harder situations are worth more:

| Tier        | Points | Stamps                                                   |
|-------------|--------|----------------------------------------------------------|
| Common      | 1      | Headshot, Flashed                                        |
| Uncommon    | 2      | Smoked, Burning, Wallbang                                |
| Rare        | 3      | Blind, On fire, No-scope, Clutch, Lightning, Arsenal     |
| Legendary   | 5      | One grenade, One shot                                    |

These are only starting values. Once there is enough data, points are calculated from how
rare each stamp is (see *Balance* in section 5). The list of stamps will grow as the project
evolves.

## 5. Comparing hands

Two hands are compared in this order:

1. **Hand type.** A full house always beats three of a kind.
2. **Points.** The sum of every stamp, weapon group and location in the hand (see below).
3. **Total damage.** The total damage dealt by all kills in the hand. Clean, dominant kills
   are worth more than finishing off wounded enemies.

Damage is always counted as health removed from the victim, capped at 100 per victim per round.

### Balance

Balance is essential: no single stamp, weapon group or location may be worth so much that
it dominates the game. CS2Holdem therefore lets **the data decide the points**:

- Every stamp, weapon group and location gets points based on **how rare it is** among all
  players' events during the season. A knife kill is rarer than a rifle kill, so it is worth
  more; a headshot is common, so it is worth little.
- Points are calculated on a **logarithmic scale with a ceiling**. Something twice as rare is
  worth a little more, not twice as much, and nothing can exceed the maximum.
- Points are **recalculated during the season** and **locked when the season ends**, together
  with the season hands.
- Until there is enough data, the starting values in section 4 are used.

This keeps the game fair even if players find something to farm: the more often something
happens, the less it is worth.

## 6. Seasons

- CS2Holdem follows the official **CS2 seasons**. Season start and end dates are configured
  in the application.
- Each player has **one season hand**, which is what competes on the leaderboard.
- The app always **suggests the strongest possible hand** automatically. Players may instead
  **build their own** hand, for example to show off a full house with a rare stamp. The hand
  the player has chosen is the one that competes.
- When a season ends, the season hand is **locked**.
- **Nothing is ever deleted.** Every season hand and every collected card are kept
  permanently, so a profile shows the player's full history across all seasons.

## 7. Watching events

Seeing a friend's full house is only half the fun; you want to **watch the kills**. Every
event, and therefore every hand, can be replayed.

| Option | How it works | Status |
|--------|--------------|--------|
| **Watch in CS2** | A button that opens the match in the CS2 client and jumps straight to the round of the event. Requires CS2 installed. | Planned |
| **2D replay** | An animated top-down map in the browser: player positions, shots, grenades, smokes and fire, a few seconds before and after each kill. Works for everyone, no game needed. | Planned |
| **Video clips** | Real video rendered by playing the demo in CS2 and recording it. Requires dedicated machines with GPUs, so it is expensive per clip. | Future idea |

Demo files store the exact **tick** of every kill (CS2 runs at 64 ticks per second) and the
**position of every player on every tick**. When a demo is parsed, CS2Holdem keeps the ticks
of each event and a short window of positions around it. The demo file itself can then be
deleted, while the event stays watchable forever.

## 8. Profile and community

- **Collection:** all cards a player has collected, filterable by season, event and stamp.
- **Season hands:** the current hand and the locked hands from previous seasons.
- **Leaderboard:** the strongest hands of the current season, and a hall of fame for past seasons.
- **Rarity:** how many players have a given hand or stamp, e.g. "only 0.4 % of players have a
  full house with One grenade this season".
- **Replays:** every card and hand on a profile can be watched, so you can see exactly how a
  friend's crazy hand happened.
- **Later:** shareable images of hands drawn as real playing cards, achievements, friends' feeds.

## 9. Limitations

- Only matches played on **Valve's official matchmaking** (Premier, Competitive, Wingman) can
  be tracked, since share codes only exist for those. FACEIT and community servers are not
  included.
- Valve only keeps demo files for a limited time, so matches must be synced regularly.
- All event data comes from the match **demo files**. A match only produces cards once its
  demo has been downloaded and parsed.

## 10. What this means for the data model

| Table              | Purpose                                                             |
|--------------------|---------------------------------------------------------------------|
| `kill_events`      | Every kill from a parsed demo: weapon, damage, tick, map area, flags. Internal raw data used to detect events, never shown on its own. |
| `events`           | The cards: one row per highlight event (player, match, round, type, damage) |
| `event_stamps`     | The stamps on each event and on each kill                           |
| `maps`, `map_areas` | Maps and their callout areas, used for location suits and straights |
| `map_area_links`   | Which areas border each other on each map, used for straights       |
| `point_values`     | Points per stamp, weapon group and location, per season             |
| `seasons`          | Season names and start/end dates                                    |
| `season_hands`     | One hand per player and season, locked when the season ends         |
| `season_hand_events` | Which events make up a season hand                                |
| `event_replays`    | Ticks and a short window of player positions, shots and grenades around each event, stored compactly (e.g. as a JSON file in S3) for the 2D replay |

The **hand evaluation engine** – detecting events in a round, finding the strongest hand among
a player's cards and comparing hands – is pure Java with no framework or database dependency.
It is built and unit tested in isolation.
