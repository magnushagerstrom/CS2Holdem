package com.robrodesign.cs2holdem.pipeline;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDemoStorageTest {

    @Test
    void storesOpensAndDeletesADemo() throws IOException {
        Path directory = Files.createTempDirectory("demos");
        LocalDemoStorage storage = new LocalDemoStorage(directory.toString());

        String key = storage.store(42L, new ByteArrayInputStream("PBDEMS2\0data".getBytes(StandardCharsets.US_ASCII)));
        try (InputStream in = storage.open(key)) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.US_ASCII)).isEqualTo("PBDEMS2\0data");
        }

        storage.delete(key);
        assertThat(Files.exists(directory.resolve(key))).isFalse();
        storage.delete(key); // deleting twice is fine
    }

    @Test
    void refusesKeysOutsideTheFolder() throws IOException {
        LocalDemoStorage storage = new LocalDemoStorage(Files.createTempDirectory("demos").toString());

        boolean refused = false;
        try {
            storage.open("../secret.txt");
        } catch (IllegalArgumentException e) {
            refused = true;
        }
        assertThat(refused).isTrue();
    }
}
