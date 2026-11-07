package com.robrodesign.cs2holdem.pipeline;

/** What the worker sends when a job fails. */
public record FailureReport(String error, boolean expired) {
}
