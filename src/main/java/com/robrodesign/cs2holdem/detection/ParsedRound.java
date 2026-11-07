package com.robrodesign.cs2holdem.detection;

/**
 * @param number      round number, starting at 1
 * @param decidedTick the tick at which the round's winner was decided. Kills after this
 *                    tick are exit frags and never count.
 */
public record ParsedRound(int number, int decidedTick) {
}
