package com.robrodesign.cs2holdem.match;

/** Var i flödet en match befinner sig. */
public enum MatchStatus {
    DISCOVERED,    // vi har share code från Steam, inget mer
    DEMO_LOCATED,  // Game Coordinator har gett oss länken till demon
    DOWNLOADED,    // demon ligger i lagringen och väntar på att parsas
    PARSED,        // korten är sparade
    EXPIRED,       // Valve har redan raderat demon, den går inte att hämta
    FAILED         // något gick fel för många gånger, se last_error
}
