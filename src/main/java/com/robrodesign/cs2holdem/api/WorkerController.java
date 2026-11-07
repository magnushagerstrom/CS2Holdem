package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.detection.ParsedMatch;
import com.robrodesign.cs2holdem.pipeline.DemoLocation;
import com.robrodesign.cs2holdem.pipeline.FailureReport;
import com.robrodesign.cs2holdem.pipeline.ImportResult;
import com.robrodesign.cs2holdem.pipeline.LocateJob;
import com.robrodesign.cs2holdem.pipeline.ParseJob;
import com.robrodesign.cs2holdem.pipeline.PipelineService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * The endpoints the demo worker talks to. Only reachable with the X-Worker-Token header
 * (see WorkerTokenFilter and SecurityConfig).
 *
 * The worker polls: "any jobs?" every few seconds. Simple, and it works through any firewall,
 * since the worker always starts the conversation.
 */
@RestController
@RequestMapping("/internal")
public class WorkerController {

    private final PipelineService pipeline;

    public WorkerController(PipelineService pipeline) {
        this.pipeline = pipeline;
    }

    @GetMapping("/jobs/locate")
    public List<LocateJob> locateJobs(@RequestParam(defaultValue = "5") int limit) {
        return pipeline.claimLocateJobs(limit);
    }

    @PostMapping("/matches/{matchId}/demo-url")
    public ResponseEntity<Void> demoLocated(@PathVariable long matchId, @RequestBody DemoLocation location) {
        pipeline.demoLocated(matchId, location.demoUrl());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/jobs/parse")
    public List<ParseJob> parseJobs(@RequestParam(defaultValue = "1") int limit) {
        return pipeline.claimParseJobs(limit);
    }

    @GetMapping("/matches/{matchId}/demo")
    public ResponseEntity<Resource> demo(@PathVariable long matchId) throws IOException {
        // InputStreamResource streams the file: a 200 MB demo is never held in memory
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(pipeline.openDemo(matchId)));
    }

    @PostMapping("/matches/{matchId}/parsed")
    public ImportResult parsed(@PathVariable long matchId, @RequestBody ParsedMatch parsed) {
        return pipeline.parsed(matchId, parsed);
    }

    @PostMapping("/matches/{matchId}/failed")
    public ResponseEntity<Void> failed(@PathVariable long matchId, @RequestBody FailureReport report) {
        pipeline.failed(matchId, report.error(), report.expired());
        return ResponseEntity.noContent().build();
    }
}
