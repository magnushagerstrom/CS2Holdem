package com.robrodesign.cs2holdem.pipeline;

/**
 * A job for the worker: ask CS2's Game Coordinator where the demo of this match is.
 * The three numbers come from the share code. IDs are strings so JavaScript cannot round them.
 */
public record LocateJob(String matchId, String shareCode, String reservationId, int tvPort) {
}
