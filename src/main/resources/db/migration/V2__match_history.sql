-- V2: Kopplar ihop en app-användare med matcherna i deras historik.
-- Behövs eftersom match_player_stats först fylls i när demon är parsad.
-- Den här tabellen säger "den här matchen finns i användarens historik" redan när vi hittat share coden.

CREATE TABLE match_history (
    id             BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    steam_id64     BIGINT       NOT NULL REFERENCES app_users (steam_id64),
    match_id       BIGINT       NOT NULL REFERENCES matches (match_id) ON DELETE CASCADE,
    discovered_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- En match kan bara finnas en gång i samma användares historik.
    -- UNIQUE skapar automatiskt ett index som börjar på steam_id64, så "hämta historiken för X" går snabbt.
    CONSTRAINT uq_match_history UNIQUE (steam_id64, match_id)
);
