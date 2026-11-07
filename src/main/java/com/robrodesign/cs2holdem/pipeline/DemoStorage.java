package com.robrodesign.cs2holdem.pipeline;

import java.io.IOException;
import java.io.InputStream;

/**
 * Where demo files are kept between upload and parsing.
 *
 * An interface, so the rest of the app does not care WHERE the files are. Locally they are
 * plain files ({@link LocalDemoStorage}); on AWS an S3 implementation can take its place
 * without any other class changing.
 */
public interface DemoStorage {

    /** Saves the demo and returns the key to find it again. */
    String store(long matchId, InputStream content) throws IOException;

    /** Opens a stored demo. The caller must close the stream. */
    InputStream open(String key) throws IOException;

    /** Removes a demo. Does nothing if it is already gone. */
    void delete(String key) throws IOException;
}
