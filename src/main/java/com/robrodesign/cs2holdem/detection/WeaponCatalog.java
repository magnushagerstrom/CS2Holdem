package com.robrodesign.cs2holdem.detection;

import com.robrodesign.cs2holdem.hand.WeaponGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Knows every weapon's group and difficulty.
 *
 * Difficulty is stored in hundredths (100 = 1.00) and these are starting values, see
 * docs/CONCEPT.md. They will later move to the database so they can be balanced without a release.
 */
public final class WeaponCatalog {

    /** @param name the weapon's canonical name, e.g. "ak47", whatever spelling it was found by */
    public record WeaponInfo(String name, WeaponGroup group, int difficulty) {
    }

    private static final Map<String, WeaponInfo> WEAPONS = new HashMap<>();

    // The demo parser reports display names ("AK-47", "Desert Eagle"). After normalising
    // (lower case, letters and digits only) they are mapped to the canonical names.
    private static final Map<String, String> ALIASES = new HashMap<>();

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

        alias("m4a4", "m4a1");          // CS2 calls the M4A4 "m4a1" internally
        alias("m4a1s", "m4a1_silencer");
        alias("galil", "galilar");
        alias("sg553", "sg556");
        alias("glock18", "glock");
        alias("usps", "usp_silencer");
        alias("p2000", "hkp2000");
        alias("cz75auto", "cz75a");
        alias("deserteagle", "deagle");
        alias("r8revolver", "revolver");
        alias("dualberettas", "elite");
        alias("mp5", "mp5sd");
        alias("ump", "ump45");
        alias("ppbizon", "bizon");
        alias("hegrenade", "hegrenade");
        alias("he", "hegrenade");
        alias("incendiarygrenade", "incgrenade");
        alias("incendiary", "incgrenade");
        alias("zeusx27", "taser");
        alias("zeus", "taser");
    }

    public static final String HE_GRENADE = "hegrenade";

    /** Weapons with a scope: a kill with one of them without zooming in is a no-scope. */
    private static final Set<String> SCOPED = Set.of("awp", "ssg08", "scar20", "g3sg1", "aug", "sg556");

    /** @param canonicalName a name from {@link WeaponInfo#name()}, e.g. "awp" */
    public static boolean hasScope(String canonicalName) {
        return SCOPED.contains(canonicalName);
    }

    private WeaponCatalog() {
    }

    private static void rifle(String name, int difficulty) {
        add(WeaponGroup.RIFLE, name, difficulty);
    }

    private static void add(WeaponGroup group, String name, int difficulty) {
        WEAPONS.put(name, new WeaponInfo(name, group, difficulty));
        ALIASES.put(normalise(name), name);
    }

    private static void alias(String normalisedAlias, String name) {
        ALIASES.put(normalisedAlias, name);
    }

    private static String normalise(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /**
     * Looks up a weapon by its internal name ("weapon_ak47", "ak47") or its display name ("AK-47").
     * Every knife skin ("knife_karambit", "Bayonet" ...) counts as a knife.
     */
    public static Optional<WeaponInfo> find(String weapon) {
        if (weapon == null || weapon.isBlank()) {
            return Optional.empty();
        }
        String lower = weapon.toLowerCase().replace("weapon_", "");
        if (lower.startsWith("knife") || lower.contains("bayonet") || lower.contains("karambit")) {
            return Optional.of(WEAPONS.get("knife"));
        }
        WeaponInfo exact = WEAPONS.get(lower);
        if (exact != null) {
            return Optional.of(exact);
        }
        String canonical = ALIASES.get(normalise(lower));
        return canonical == null ? Optional.empty() : Optional.ofNullable(WEAPONS.get(canonical));
    }
}
