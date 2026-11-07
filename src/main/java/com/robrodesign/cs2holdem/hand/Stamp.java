package com.robrodesign.cs2holdem.hand;

public enum Stamp {


    // Kill stamps: the enemy was impaired
    FLASHED("Flashed", 1, Scope.KILL),
    SMOKED("Smoked", 2, Scope.KILL),
    BURNING("Burning", 2, Scope.KILL),

    // Kill stamps: the player was impaired
    BLIND("Blind", 3, Scope.KILL),
    ON_FIRE("On fire", 3, Scope.KILL),

    // Kill stamps: skill shots
    HEADSHOT("Headshot", 1, Scope.KILL),
    WALLBANG("Wallbang", 2, Scope.KILL),
    NO_SCOPE("No-scope", 5, Scope.KILL),

    // Event stamps
    ONE_GRENADE("One grenade", 5, Scope.EVENT),
    ONE_SHOT("One shot", 5, Scope.EVENT),
    CLUTCH("Clutch", 3, Scope.EVENT),
    LIGHTNING("Lightning", 3, Scope.EVENT);

    /**
     * Where a stamp belongs: on a single kill or on a whole event.
     */
    public enum Scope {
        KILL, EVENT
    }

    private final String displayName;
    private final int points;
    private final Scope scope;

    Stamp(String displayName, int points, Scope scope) {
        this.displayName = displayName;
        this.points = points;
        this.scope = scope;
    }

    public String displayName() {
        return displayName;
    }

    public int points() {
        return points;
    }

    public Scope scope() {
        return scope;
    }
}

