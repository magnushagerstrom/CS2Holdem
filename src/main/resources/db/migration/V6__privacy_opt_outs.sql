-- Spelare som har bett om att bli raderade. Nya matcher sparar inget om dem.
-- Ingen koppling till players: den raden är just raderad, men SteamID:t måste vi komma ihåg.
CREATE TABLE privacy_opt_outs (
    steam_id64    BIGINT       PRIMARY KEY,
    opted_out_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
