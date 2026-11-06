-- V1: Grundstrukturen för CS2Holdem.
-- Den här filen får ALDRIG ändras när den har körts. Nya ändringar = ny fil (V2__..., V3__...).

-- Alla Steam-spelare som någonsin dykt upp i en match, oavsett om de använder appen eller inte.
CREATE TABLE players (
    steam_id64    BIGINT       PRIMARY KEY,
    display_name  VARCHAR(64),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- De spelare som har registrerat sig i appen och gett oss tillgång till sin matchhistorik.
-- Primärnyckeln är samma som i players: varje app-användare ÄR en spelare (1-till-1).
CREATE TABLE app_users (
    steam_id64              BIGINT        PRIMARY KEY REFERENCES players (steam_id64),
    auth_code               VARCHAR(512)  NOT NULL,   -- krypteras i steg 2
    last_known_share_code   VARCHAR(40)   NOT NULL,   -- bokmärket: senaste matchen vi känner till
    last_synced_at          TIMESTAMPTZ,
    next_sync_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Synk-jobbet letar efter användare vars tur det är: WHERE next_sync_at <= now()
CREATE INDEX idx_app_users_next_sync_at ON app_users (next_sync_at);

-- En match, som den ser ut globalt. Sparas en gång även om tio app-användare spelade i den.
CREATE TABLE matches (
    match_id          BIGINT       PRIMARY KEY,           -- avkodat ur share code
    share_code        VARCHAR(40)  NOT NULL UNIQUE,       -- CSGO-xxxxx-xxxxx-...
    status            VARCHAR(20)  NOT NULL,              -- DISCOVERED, DOWNLOADED, PARSED, FAILED
    map_name          VARCHAR(32),                        -- okänt tills demon parsats
    played_at         TIMESTAMPTZ,
    duration_seconds  INTEGER,
    team1_score       INTEGER,
    team2_score       INTEGER,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_matches_status ON matches (status);

-- En spelares prestation i en match. Vi sparar RÅA siffror (total skada, antal headshots)
-- och räknar fram ADR och HS% när vi visar dem, så blir inget fel av avrundningar.
CREATE TABLE match_player_stats (
    id              BIGINT   GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    match_id        BIGINT   NOT NULL REFERENCES matches (match_id) ON DELETE CASCADE,
    steam_id64      BIGINT   NOT NULL REFERENCES players (steam_id64),
    team            INTEGER  NOT NULL CHECK (team IN (1, 2)),
    kills           INTEGER  NOT NULL DEFAULT 0,
    deaths          INTEGER  NOT NULL DEFAULT 0,
    assists         INTEGER  NOT NULL DEFAULT 0,
    headshot_kills  INTEGER  NOT NULL DEFAULT 0,
    damage_total    INTEGER  NOT NULL DEFAULT 0,
    rounds_played   INTEGER  NOT NULL DEFAULT 0,
    mvps            INTEGER  NOT NULL DEFAULT 0,
    CONSTRAINT uq_match_player UNIQUE (match_id, steam_id64)
);

-- "Visa alla matcher för spelare X" är den vanligaste frågan i hela appen.
CREATE INDEX idx_match_player_stats_steam_id64 ON match_player_stats (steam_id64);
