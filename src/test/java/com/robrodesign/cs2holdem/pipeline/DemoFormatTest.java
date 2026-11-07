package com.robrodesign.cs2holdem.pipeline;

import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DemoFormatTest {

    private static InputStream stream(String content) {
        return new BufferedInputStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1)));
    }

    @Test
    void recognisesACs2Demo() throws IOException {
        assertThat(DemoFormat.looksLikeDemo(stream("PBDEMS2\0rest of the demo"))).isTrue();
    }

    @Test
    void recognisesABzip2CompressedDemo() throws IOException {
        assertThat(DemoFormat.looksLikeDemo(stream("BZh91AY&SY..."))).isTrue();
    }

    @Test
    void rejectsOtherFiles() throws IOException {
        assertThat(DemoFormat.looksLikeDemo(stream("HL2DEMO\0 an old CS:GO demo"))).isFalse();
        assertThat(DemoFormat.looksLikeDemo(stream("<html>"))).isFalse();
        assertThat(DemoFormat.looksLikeDemo(stream(""))).isFalse();
    }

    @Test
    void leavesTheStreamAtTheStart() throws IOException {
        InputStream in = stream("PBDEMS2\0abc");
        DemoFormat.looksLikeDemo(in);

        assertThat(new String(in.readAllBytes(), StandardCharsets.ISO_8859_1)).isEqualTo("PBDEMS2\0abc");
    }
}
