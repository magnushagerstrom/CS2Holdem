-- Mer statistik per spelare och match. Fortfarande bara RÅA summor: procent och snitt
-- (HS %, ADR, träffprocent, time to kill) räknas fram när de visas.
ALTER TABLE match_player_stats
    ADD COLUMN result          VARCHAR(4) NOT NULL DEFAULT 'DRAW' CHECK (result IN ('WIN', 'LOSS', 'DRAW')),
    ADD COLUMN flash_assists   INTEGER    NOT NULL DEFAULT 0,
    ADD COLUMN utility_damage  INTEGER    NOT NULL DEFAULT 0,
    ADD COLUMN shots_fired     INTEGER    NOT NULL DEFAULT 0,
    ADD COLUMN shots_hit       INTEGER    NOT NULL DEFAULT 0,
    ADD COLUMN opening_kills   INTEGER    NOT NULL DEFAULT 0,
    ADD COLUMN opening_deaths  INTEGER    NOT NULL DEFAULT 0,
    -- Time to kill: summan av alla mätbara kills tid (i ticks, 64 per sekund) och hur många de var
    ADD COLUMN ttk_ticks       BIGINT     NOT NULL DEFAULT 0,
    ADD COLUMN ttk_kills       INTEGER    NOT NULL DEFAULT 0;
