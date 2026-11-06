package com.robrodesign.cs2holdem.hand;

public enum WeaponGroup {
    RIFLE("Rifle"),
    SNIPER_RIFLE("Sniper rifle"),
    PISTOL("Pistol"),
    SMG("SMG"),
    SHOTGUN("Shotgun"),
    HEAVY_WEAPON("Heavy weapon"),
    GRENADE("Grenade"),
    MELEE("Melee");


    private final String displayName;

    WeaponGroup(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}