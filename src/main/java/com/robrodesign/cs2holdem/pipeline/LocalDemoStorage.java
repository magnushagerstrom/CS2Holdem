package com.robrodesign.cs2holdem.pipeline;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Keeps demos as files in a folder on this computer (default ./data/demos). */
@Component
public class LocalDemoStorage implements DemoStorage {

    private final Path directory;

    public LocalDemoStorage(@Value("${cs2holdem.demos.directory:./data/demos}") String directory) throws IOException {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(this.directory);
    }

    @Override
    public String store(long matchId, InputStream content) throws IOException {
        String key = matchId + ".dem";
        // Write to a temporary name first and rename when done, so a half-written file
        // is never mistaken for a complete demo.
        Path temp = Files.createTempFile(directory, matchId + "-", ".part");
        try {
            Files.copy(content, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, resolve(key), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temp);
        }
        return key;
    }

    @Override
    public InputStream open(String key) throws IOException {
        return Files.newInputStream(resolve(key));
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolve(key));
    }

    // Keys come from our own database, but checking costs nothing: a key like "../../x"
    // must never point outside the demo folder.
    private Path resolve(String key) {
        Path path = directory.resolve(key).normalize();
        if (!path.startsWith(directory)) {
            throw new IllegalArgumentException("Invalid demo key");
        }
        return path;
    }
}
