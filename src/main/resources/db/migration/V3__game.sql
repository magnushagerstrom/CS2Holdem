-- V3: Spelet. Kort, kills, säsonger och säsongshänder.
-- Regler och begrepp: docs/CONCEPT.md

-- Det som behövs för att ladda ner och följa upp en matchs demo.
ALTER TABLE matches
    ADD COLUMN reservation_id BIGINT,
    ADD COLUMN tv_port        INTEGER,
    ADD COLUMN demo_url       TEXT,
    ADD COLUMN demo_key       TEXT,
    ADD COLUMN attempts       INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_error     TEXT,
    ADD COLUMN claimed_at     TIMESTAMPTZ;   -- när en worker tog jobbet; NULL = ledigt

-- Workern frågar "ge mig matcher med status X som ingen jobbar med". Indexet gör det snabbt.
CREATE INDEX idx_matches_status_created_at ON matches (status, created_at);

-- En CS2-säsong. Bara en hand per spelare och säsong tävlar, och den låses när säsongen tar slut.
CREATE TABLE seasons (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(64)  NOT NULL UNIQUE,
    starts_at  TIMESTAMPTZ  NOT NULL,
    ends_at    TIMESTAMPTZ  NOT NULL,
    locked     BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_season_dates CHECK (ends_at > starts_at)
);

-- Startsäsong. Ändra datumen så att de stämmer med Valves officiella säsonger (ny migrering!).
INSERT INTO seasons (name, starts_at, ends_at)
VALUES ('Season 1', '2026-01-01T00:00:00Z', '2027-01-01T00:00:00Z');

-- Korten: en händelse (dubbelkill, ace, knivkill ...) från en runda.
CREATE TABLE cards (
    id            BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    steam_id64    BIGINT        NOT NULL REFERENCES players (steam_id64),
    match_id      BIGINT        NOT NULL REFERENCES matches (match_id) ON DELETE CASCADE,
    round_number  INTEGER       NOT NULL,
    type          VARCHAR(20)   NOT NULL,
    stamps        VARCHAR(200)  NOT NULL DEFAULT '',
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- En runda ger högst en händelse per spelare.
    CONSTRAINT uq_card_round UNIQUE (steam_id64, match_id, round_number)
);

-- Varje kill som räknas. Kills som ingår i ett kort pekar på kortet.
-- Stegar och färger byggs av alla kills, även de som inte är kort.
CREATE TABLE kills (
    id            BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    steam_id64    BIGINT        NOT NULL REFERENCES players (steam_id64),
    match_id      BIGINT        NOT NULL REFERENCES matches (match_id) ON DELETE CASCADE,
    round_number  INTEGER       NOT NULL,
    tick          INTEGER       NOT NULL,
    weapon_group  VARCHAR(20)   NOT NULL,
    difficulty    INTEGER       NOT NULL,
    damage        INTEGER       NOT NULL,
    map_area      VARCHAR(64)   NOT NULL,
    stamps        VARCHAR(200)  NOT NULL DEFAULT '',
    card_id       BIGINT        REFERENCES cards (id) ON DELETE SET NULL
);

CREATE INDEX idx_cards_player_match ON cards (steam_id64, match_id);
CREATE INDEX idx_kills_player_match ON kills (steam_id64, match_id);
CREATE INDEX idx_kills_card ON kills (card_id);

-- En spelares hand för en säsong. custom = spelaren har byggt den själv.
CREATE TABLE season_hands (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    steam_id64    BIGINT       NOT NULL REFERENCES players (steam_id64),
    season_id     BIGINT       NOT NULL REFERENCES seasons (id),
    hand_type     VARCHAR(20)  NOT NULL,
    score         INTEGER      NOT NULL,
    total_damage  INTEGER      NOT NULL,
    custom        BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_season_hand UNIQUE (steam_id64, season_id)
);

-- Topplistan sorterar på poäng och sedan damage inom en säsong.
CREATE INDEX idx_season_hands_leaderboard ON season_hands (season_id, score DESC, total_damage DESC);

-- Vad en säsongshand består av: antingen ett kort eller en kill (stege och färg).
CREATE TABLE season_hand_items (
    id              BIGINT   GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    season_hand_id  BIGINT   NOT NULL REFERENCES season_hands (id) ON DELETE CASCADE,
    position        INTEGER  NOT NULL,
    card_id         BIGINT   REFERENCES cards (id) ON DELETE CASCADE,
    kill_id         BIGINT   REFERENCES kills (id) ON DELETE CASCADE,
    CONSTRAINT ck_item_card_or_kill CHECK ((card_id IS NULL) <> (kill_id IS NULL))
);

CREATE INDEX idx_season_hand_items_hand ON season_hand_items (season_hand_id);
