# CS2Holdem

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791)
![Maven](https://img.shields.io/badge/build-Maven-C71A36)
![Status](https://img.shields.io/badge/status-in%20development-yellow)

**Collect the best moments of your CS2 season and play them as a poker hand.**

CS2Holdem turns your Counter-Strike 2 highlights into playing cards. Every double kill,
triple kill, ace or knife kill you make during a season becomes a card in your collection.
From those cards you build the strongest poker hand you can: a triple kill and a double kill
make a **full house**, an ace makes **five of a kind**, and a perfect ace is a **royal flush**. Stamps such as *Smoked* or *One shot*
show how spectacular each moment was, and when the CS2 season ends, the strongest hand wins.

Players connect their Steam account once. From then on CS2Holdem discovers every new match
they play, downloads the demo and finds the highlight moments in it.

The project is built as a production-style backend from day one: versioned database
migrations, a clean module structure, secrets kept out of the code, and an architecture that
can grow from one user to thousands without being rewritten.

---

## Contents

- [How it works](#how-it-works)
- [Features](#features)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Engineering decisions](#engineering-decisions)
- [Data model](#data-model)
- [API](#api)
- [Getting started](#getting-started)
- [Project structure](#project-structure)
- [Roadmap](#roadmap)
- [About the author](#about-the-author)

---

## How it works

| Poker      | CS2Holdem                                                              |
|------------|------------------------------------------------------------------------|
| Card       | A highlight event from one round: double kill, triple kill, ace, ...   |
| Hand       | Up to five kills' worth of events from the same season                 |
| Hand type  | Pair (2k), two pair, three of a kind (3k), full house (3k + 2k), four of a kind (4k), five of a kind (ace), royal flush (a perfect ace: all headshots or 500 damage) |
| Stamps     | Extra points per kill or event: the enemy was *Flashed*, *Smoked* or *Burning*, you were *Blind* or *On fire*, *Headshot*, *Wallbang*, *One grenade*, *One shot*, ... |
| Tiebreaker | Hand type first, then stamp points, then total damage                  |
| Game       | One CS2 season. Each player has one season hand; the strongest one wins |

**Example:** a round where one HE grenade kills three (*One grenade*) and another round where
one AWP shot kills two (*One shot*) together make an exceptionally strong **full house**.

The app always suggests the strongest possible hand, but players can build their own.
Every season hand and every card is kept permanently on the player's profile.

The full rules are in **[docs/CONCEPT.md](docs/CONCEPT.md)**.

## Features

**Working today**

- Register with a SteamID64, a CS2 match-history auth code and one known share code
- Credentials are verified against the Steam Web API before an account is created
- Full match history is discovered by following Valve's chain of share codes
- Share codes are decoded into match ID, reservation ID and TV port
- Manual re-sync and a match list per user through a REST API
- Consistent error responses following RFC 9457 (Problem Details), including per-field validation errors

**Planned**

- Demo download through the CS2 Game Coordinator
- Demo parsing into individual kills: weapon, damage, round and circumstances
- Event detection: double kills, triple kills, aces and special kills per round
- Stamps such as *Smoked*, *Wallbang*, *One grenade* and *One shot*
- Hand evaluation engine that finds and compares the strongest hands
- Seasons, season hands, leaderboards and a permanent card collection
- Encrypted storage of users' auth codes
- Scheduled background sync that respects Steam's API rate limits
- Deployment on AWS

## Tech stack

| Area          | Choice                                                  |
|---------------|---------------------------------------------------------|
| Language      | Java 21                                                 |
| Framework     | Spring Boot 4 (Web MVC, Data JPA, Validation, RestClient) |
| Build         | Maven                                                   |
| Database      | PostgreSQL 16, schema managed by Flyway                 |
| ORM           | Hibernate 7 (schema validation only)                    |
| Local env     | Docker Compose, started automatically by Spring Boot    |
| Testing       | JUnit 5, AssertJ                                        |
| Cloud (planned) | AWS: RDS, S3, SQS, ECS, Secrets Manager               |

## Architecture

```mermaid
flowchart LR
    client([Client]) -->|REST| api

    subgraph app [CS2Holdem - Spring Boot]
        api[api<br/>controllers, DTOs,<br/>error handling]
        account[account<br/>registration]
        sync[sync<br/>match discovery]
        match[match<br/>matches & stats]
        steam[steam<br/>Steam Web API client,<br/>share code decoding]
        api --> account
        api --> sync
        sync --> account
        sync --> match
        sync --> steam
        account --> steam
    end

    app --> db[(PostgreSQL)]
    steam -->|HTTPS| valve[Steam Web API]
    app -.->|planned| queue[[Job queue]]
    queue -.-> worker[Demo worker<br/>download → S3 → parse]
    worker -.-> db
```

The application is a **modular monolith**: one deployable unit, organised by feature
rather than by technical layer. Modules depend on each other in one direction only, so any
of them can later be extracted into its own service without touching the rest.

## Engineering decisions

A few choices worth calling out, and the reasoning behind them:

- **Flyway owns the schema, Hibernate only validates it.** Every database change is a
  reviewed SQL file in version control (`V1__...`, `V2__...`). Hibernate runs with
  `ddl-auto: validate`, so the app refuses to start if the Java entities and the database
  ever drift apart.
- **External calls happen outside database transactions.** Registration verifies the user
  with Steam first, then saves the player and account in a single short transaction. A slow
  Steam response never holds a database connection.
- **Concurrency is handled, not hoped for.** When two teammates sync at the same moment and
  both discover the same match, the database's primary key rejects the duplicate and the
  sync carries on.
- **Rate-limit aware scheduling.** Each user gets a `next_sync_at` based on activity: soon
  when more matches are waiting, later when nothing new was found. This is the foundation
  for staying within Steam's daily API quota at scale.
- **64-bit IDs are JSON strings.** SteamIDs and match IDs exceed 2^53, the largest integer
  JavaScript can represent exactly, so the API never sends them as numbers.
- **Secrets never touch the codebase.** Database credentials and the Steam API key live in
  a git-ignored `.env` locally and in environment variables in production. Sensitive values
  are masked in `toString()` so they cannot leak into logs.
- **Entities protect their own rules.** No public setters; state changes go through named
  methods such as `match.markParsed(...)`. JPA's required no-args constructors are
  `protected` so they cannot be misused.
- **Raw numbers are stored, ratios are derived.** Total damage and headshot kills are
  persisted; ADR and HS% are calculated, so averages across many matches stay exact.

## Data model

| Table                | Purpose                                                                 |
|----------------------|-------------------------------------------------------------------------|
| `players`            | Every Steam player seen in any match, registered or not                 |
| `app_users`          | Players who signed up; holds their auth code and sync schedule          |
| `matches`            | One row per match, shared by everyone who played in it                  |
| `match_history`      | Which matches belong to a registered user's history                     |
| `match_player_stats` | One player's performance in one match                                   |

Next up: tables for kills, events (the cards), stamps, seasons and season hands.
See the data model section in [docs/CONCEPT.md](docs/CONCEPT.md#9-what-this-means-for-the-data-model).

## API

| Method | Path                                | Description                                       |
|--------|-------------------------------------|---------------------------------------------------|
| POST   | `/api/accounts`                     | Register and discover match history               |
| POST   | `/api/accounts/{steamId64}/sync`    | Fetch new matches from Steam now                  |
| GET    | `/api/accounts/{steamId64}/matches` | List matches in the user's history, newest first  |

Example:

```http
POST /api/accounts
Content-Type: application/json

{
  "steamId64": "76561197960287930",
  "authCode": "ABCD-EFGHJ-KLMN",
  "knownShareCode": "CSGO-GADqf-jjyJ8-cSP2r-smZRo-TO2xK"
}
```

```json
HTTP/1.1 201 Created

{ "steamId64": "76561197960287930", "matchesFound": 4 }
```

Validation errors explain exactly what is wrong:

```json
HTTP/1.1 400 Bad Request

{
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "errors": {
    "authCode": "must look like XXXX-XXXXX-XXXX",
    "steamId64": "must be a 17-digit SteamID64"
  }
}
```

Ready-to-run requests for IntelliJ's HTTP client are in [`http/accounts.http`](http/accounts.http).

## Getting started

**Requirements:** JDK 21 and Docker Desktop.

1. Copy the environment template and fill in your own values:
   ```bash
   cp .env.example .env
   ```
   You need a Steam Web API key from <https://steamcommunity.com/dev/apikey>.
2. Start the application:
   ```bash
   ./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
   ```
   Spring Boot starts PostgreSQL from `docker-compose.yml`, connects to it automatically,
   and Flyway creates the schema. The API is available on `http://localhost:8080`.
3. To register yourself you need a match-history **auth code**, which you can create on
   [Steam's CS2 match history page](https://help.steampowered.com/en/wizard/HelpWithGameIssue/?appid=730&issueid=128),
   and one **share code** from a recent match, found in-game under *Watch → Your Matches*.

Run the tests (Docker must be running for the integration test):

```bash
./mvnw test
```

## Project structure

```
src/main/java/com/robrodesign/cs2holdem
├── account/   registered users and registration
├── api/       REST controllers, request/response DTOs, global error handling
├── match/     matches, match history and per-player statistics
├── player/    every Steam player seen in a match
├── steam/     Steam Web API client and share code decoding
└── sync/      discovery of new matches

src/main/resources
├── application.yaml
└── db/migration/   Flyway migrations (V1, V2, ...)
```

## Roadmap

- [x] Project foundation: schema, migrations and local environment
- [x] Register users and discover match history via the Steam Web API
- [ ] Encrypt stored auth codes
- [ ] Scheduled sync within Steam's API rate limits
- [ ] Fetch demo URLs from the CS2 Game Coordinator
- [ ] Hand evaluation engine in pure Java (events, hands, comparison)
- [ ] Demo worker: extract every kill from a demo and detect events
- [ ] Stamps
- [ ] Seasons, season hands and leaderboards
- [ ] CI pipeline with GitHub Actions
- [ ] Deploy to AWS

## About the author

Hi, I'm **Magnus**, a Java developer in training based in Stockholm.

I'm studying **Java Developer with a Cloud Focus** (*Javautvecklare med molninriktning*),
a full-time higher vocational education program covering Java, Spring, databases and
cloud development on AWS.

CS2Holdem started from two things I care about: I play a lot of Counter-Strike 2, and I
wanted a project that goes beyond course assignments. The idea of collecting your highlight
moments and playing them as a poker hand is my own, and building it means solving real backend problems: external APIs,
processing large binary files, data modelling, concurrency, security and scalability. I build it step by step and try to make every decision one I can explain.

Outside of code I'm a hands-on maker: CNC machining, 3D printing and laser engraving.

- GitHub: [@magnushagerstrom](https://github.com/magnushagerstrom)

---

*CS2Holdem is an independent project and is not affiliated with or endorsed by Valve
Corporation. Counter-Strike and Steam are trademarks of Valve Corporation.*
