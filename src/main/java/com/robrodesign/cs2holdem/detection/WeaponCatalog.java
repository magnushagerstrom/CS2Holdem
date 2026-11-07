package com.robrodesign.cs2holdem.detection;

import com.robrodesign.cs2holdem.hand.WeaponGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Knows every weapon's group and difficulty.
 *
 * Difficulty is stored in hundredths (100 = 1.00) and these are starting values, see
 * docs/CONCEPT.md. They will later move to the database so they can be balanced without a release.
 */
public final class WeaponCatalog {

    public record WeaponInfo(WeaponGroup group, int difficulty) {
    }

    private static final Map<String, WeaponInfo> WEAPONS = new HashMap<>();

    static {
        rifle("ak47", 115);
        rifle("m4a1", 110);
        rifle("m4a1_silencer", 115);
        rifle("famas", 110);
        rifle("galilar", 110);
        rifle("aug", 105);
        rifle("sg556", 105);

        add(WeaponGroup.SNIPER_RIFLE, "awp", 140);
        add(WeaponGroup.SNIPER_RIFLE, "ssg08", 150);
        add(WeaponGroup.SNIPER_RIFLE, "scar20", 120);
        add(WeaponGroup.SNIPER_RIFLE, "g3sg1", 120);

        add(WeaponGroup.PISTOL, "glock", 120);
        add(WeaponGroup.PISTOL, "usp_silencer", 125);
        add(WeaponGroup.PISTOL, "hkp2000", 125);
        add(WeaponGroup.PISTOL, "p250", 125);
        add(WeaponGroup.PISTOL, "fiveseven", 125);
        add(WeaponGroup.PISTOL, "tec9", 120);
        add(WeaponGroup.PISTOL, "cz75a", 120);
        add(WeaponGroup.PISTOL, "deagle", 150);
        add(WeaponGroup.PISTOL, "revolver", 160);
        add(WeaponGroup.PISTOL, "elite", 130);

        add(WeaponGroup.SMG, "mac10", 110);
        add(WeaponGroup.SMG, "mp9", 110);
        add(WeaponGroup.SMG, "mp7", 110);
        add(WeaponGroup.SMG, "mp5sd", 110);
        add(WeaponGroup.SMG, "ump45", 110);
        add(WeaponGroup.SMG, "p90", 100);
        add(WeaponGroup.SMG, "bizon", 105);

        add(WeaponGroup.SHOTGUN, "nova", 115);
        add(WeaponGroup.SHOTGUN, "xm1014", 110);
        add(WeaponGroup.SHOTGUN, "sawedoff", 120);
        add(WeaponGroup.SHOTGUN, "mag7", 115);

        add(WeaponGroup.HEAVY_WEAPON, "m249", 105);
        add(WeaponGroup.HEAVY_WEAPON, "negev", 100);

        add(WeaponGroup.GRENADE, "hegrenade", 170);
        add(WeaponGroup.GRENADE, "molotov", 160);
        add(WeaponGroup.GRENADE, "incgrenade", 160);
        add(WeaponGroup.GRENADE, "inferno", 160);

        add(WeaponGroup.MELEE, "knife", 200);
        add(WeaponGroup.MELEE, "taser", 200);
    }

    public static final String HE_GRENADE = "hegrenade";

    private WeaponCatalog() {
    }

    private static void rifle(String name, int difficulty) {
        add(WeaponGroup.RIFLE, name, difficulty);
    }

    private static void add(WeaponGroup group, String name, int difficulty) {
        WEAPONS.put(name, new WeaponInfo(group, difficulty));
    }

    /**
     * Looks up a weapon. Names are matched without the "weapon_" prefix, and every knife skin
     * ("knife_karambit", "bayonet" ...) counts as a knife.
     */
    public static Optional<WeaponInfo> find(String weapon) {
        if (weapon == null || weapon.isBlank()) {
            return Optional.empty();
        }
        String name = weapon.toLowerCase().replace("weapon_", "");
        if (name.startsWith("knife") || name.equals("bayonet")) {
            name = "knife";
        }
        return Optional.ofNullable(WEAPONS.get(name));
    }
}
