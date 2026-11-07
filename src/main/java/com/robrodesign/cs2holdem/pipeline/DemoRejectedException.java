package com.robrodesign.cs2holdem.pipeline;

/** An uploaded file that is not a CS2 demo, or a demo for a match that cannot take one. */
public class DemoRejectedException extends RuntimeException {
    public DemoRejectedException(String message) {
        super(message);
    }
}
