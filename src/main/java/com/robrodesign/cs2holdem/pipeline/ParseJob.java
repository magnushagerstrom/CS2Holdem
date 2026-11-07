package com.robrodesign.cs2holdem.pipeline;

/**
 * A job for the worker: read the demo of this match and send back what happened.
 *
 * @param demoUrl  Valve's download link, or null when the demo was uploaded to us.
 *                 Then the worker fetches it from GET /internal/matches/{matchId}/demo.
 */
public record ParseJob(String matchId, String demoUrl) {
}
