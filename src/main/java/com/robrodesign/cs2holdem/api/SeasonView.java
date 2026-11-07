package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.season.Season;

import java.time.Instant;

public record SeasonView(String id, String name, Instant startsAt, Instant endsAt, boolean locked) {

    public static SeasonView from(Season season) {
        return new SeasonView(String.valueOf(season.getId()), season.getName(), season.getStartsAt(),
                season.getEndsAt(), season.isLocked());
    }
}
