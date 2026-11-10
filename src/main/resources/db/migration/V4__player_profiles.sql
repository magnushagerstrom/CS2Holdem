-- Spelarnas Steam-profil: namnet (display_name finns redan) och avataren.
-- profile_checked_at säger när vi senast frågade Steam. NULL = aldrig, och då frågar vi snart.
ALTER TABLE players
    ADD COLUMN avatar_url         VARCHAR(255),
    ADD COLUMN profile_checked_at TIMESTAMPTZ;

CREATE INDEX idx_players_profile_checked_at ON players (profile_checked_at);
