-- Vilken version av poängreglerna en sparad hand räknades med.
-- Version 2: kill-poäng = damage x svårighet (förut damage / 10 x svårighet), allt annat x 10.
-- Händer med lägre version räknas om när appen startar, se ScoreVersionUpgrade.
ALTER TABLE season_hands
    ADD COLUMN score_version INTEGER NOT NULL DEFAULT 1;
