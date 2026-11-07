package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.pipeline.DemoRejectedException;
import com.robrodesign.cs2holdem.pipeline.PipelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/** Things a logged-in player can do with a match. */
@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final PipelineService pipeline;
    private final CurrentPlayer currentPlayer;

    public MatchController(PipelineService pipeline, CurrentPlayer currentPlayer) {
        this.pipeline = pipeline;
        this.currentPlayer = currentPlayer;
    }

    /**
     * Upload the demo of a match yourself (multipart form field "file").
     * Answers 202 Accepted: the demo is stored now and read by the worker shortly.
     */
    @PostMapping("/{matchId}/demo")
    public ResponseEntity<Void> uploadDemo(@PathVariable long matchId,
                                           @RequestParam("file") MultipartFile file) throws IOException {
        long steamId64 = currentPlayer.steamId64().orElseThrow(NotYourAccountException::new);
        if (file.isEmpty()) {
            throw new DemoRejectedException("The file is empty");
        }
        try (InputStream content = file.getInputStream()) {
            pipeline.uploadDemo(steamId64, matchId, content);
        }
        return ResponseEntity.accepted().build();
    }
}
