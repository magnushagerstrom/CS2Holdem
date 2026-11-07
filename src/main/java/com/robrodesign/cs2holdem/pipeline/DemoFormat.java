package com.robrodesign.cs2holdem.pipeline;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Recognises a CS2 demo by its first bytes ("magic bytes"), without trusting the file name.
 *
 * - "PBDEMS2\0": a CS2 demo (.dem)
 * - "BZh": bzip2-compressed, which is how Valve serves match demos (.dem.bz2)
 */
public final class DemoFormat {

    private static final byte[] CS2_DEMO = "PBDEMS2\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] BZIP2 = "BZh".getBytes(StandardCharsets.US_ASCII);

    private DemoFormat() {
    }

    /** Reads the first bytes of the stream. The stream must support mark/reset. */
    public static boolean looksLikeDemo(InputStream in) throws IOException {
        if (!in.markSupported()) {
            throw new IllegalArgumentException("The stream must support mark/reset");
        }
        in.mark(CS2_DEMO.length);
        byte[] head = in.readNBytes(CS2_DEMO.length);
        in.reset();
        return startsWith(head, CS2_DEMO) || startsWith(head, BZIP2);
    }

    static boolean startsWith(byte[] data, byte[] prefix) {
        return data.length >= prefix.length && Arrays.equals(data, 0, prefix.length, prefix, 0, prefix.length);
    }
}
