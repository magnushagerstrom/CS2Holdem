package com.robrodesign.cs2holdem.match;

/** Var i flödet en match befinner sig. */
public enum MatchStatus {
    DISCOVERED,  // vi har share code från Steam, inget mer
    DOWNLOADED,  // demon ligger i lagringen (S3)
    PARSED,      // statistiken är sparad
    FAILED       // något gick fel, se loggar
}
