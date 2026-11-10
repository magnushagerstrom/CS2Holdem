// Command worker is CS2Holdem's demo worker.
//
// It asks the Java server for jobs, downloads CS2 demos, reads every kill with the
// demoinfocs-golang library and sends the result back. It runs as its own program so
// that reading 100 MB demo files never slows down the web server, and so that more
// workers can be started when there is more work.
//
// Configuration (environment variables):
//
//	CS2HOLDEM_URL   the server, default http://localhost:8080
//	WORKER_TOKEN    the shared secret, the same value as in the server's .env
//	POLL_SECONDS    how long to wait when there is no work, default 10
//	WORKERS         how many matches to work on at the same time, default 3
package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"strconv"
	"sync"
	"sync/atomic"
	"syscall"
	"time"
)

func main() {
	token := os.Getenv("WORKER_TOKEN")
	if token == "" {
		log.Fatal("WORKER_TOKEN is not set")
	}
	api := NewAPI(envOr("CS2HOLDEM_URL", "http://localhost:8080"), token)
	poll := time.Duration(envInt("POLL_SECONDS", 10)) * time.Second

	// Stop cleanly on Ctrl+C or when Docker/ECS stops the container
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	workers := envInt("WORKERS", 3)

	// Each worker is a goroutine: a function running at the same time as the others.
	// Most of a job is waiting for Valve's slow download, so several at once finish the
	// queue several times faster. They never take the same match: the server hands out
	// each job to one caller only (FOR UPDATE SKIP LOCKED).
	log.Printf("starting %d workers, polling %s", workers, api.baseURL)
	var wg sync.WaitGroup
	for id := 1; id <= workers; id++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			loop(ctx, api, poll, id)
		}()
	}
	wg.Wait() // until Ctrl+C has stopped every worker
	log.Print("all workers stopped")
}

// loop is one worker: take a job, do it, and wait a while when there is nothing to do.
func loop(ctx context.Context, api *API, poll time.Duration, id int) {
	for ctx.Err() == nil {
		worked, err := runOnce(ctx, api, id)
		if err != nil && ctx.Err() == nil {
			log.Printf("[worker %d] error: %v", id, err)
		}
		if !worked {
			select {
			case <-ctx.Done():
			case <-time.After(poll):
			}
		}
	}
}

// idle is true while the queue is empty, so "all done" is logged once, not by every worker.
// atomic.Bool can be read and changed by several goroutines at once without mixing up.
var idle atomic.Bool

// runOnce takes at most one parse job. It returns true if there was a job.
func runOnce(ctx context.Context, api *API, id int) (bool, error) {
	// Demo links come from the Java server's locator (CS2 Game Coordinator) or from uploads.
	jobs, err := api.ParseJobs(ctx, 1)
	if err != nil || len(jobs) == 0 {
		// CompareAndSwap(false, true) succeeds for exactly one worker: the first to find the
		// queue empty. The others see idle already true and stay quiet.
		if err == nil && idle.CompareAndSwap(false, true) {
			log.Print("all done, waiting for new matches")
		}
		return false, err
	}
	idle.Store(false)
	job := jobs[0]
	log.Printf("[worker %d] parsing match %s", id, job.MatchID)

	parsed, err := parseJob(ctx, api, job)
	if err != nil {
		log.Printf("[worker %d] match %s failed: %v", id, job.MatchID, err)
		_, expired := err.(expiredError)
		return true, api.ReportFailure(ctx, job.MatchID, err.Error(), expired)
	}
	result, err := api.ReportParsed(ctx, job.MatchID, parsed)
	if err != nil {
		return true, err
	}
	log.Printf("[worker %d] match %s done: %s", id, job.MatchID, result)
	return true, nil
}

func parseJob(ctx context.Context, api *API, job ParseJob) (*ParsedMatch, error) {
	demo, err := downloadDemo(ctx, api, job)
	if err != nil {
		return nil, err
	}
	defer demo.Close()

	matchID, err := strconv.ParseInt(job.MatchID, 10, 64)
	if err != nil {
		return nil, err
	}
	return ParseDemo(matchID, demo)
}

func envOr(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

func envInt(key string, fallback int) int {
	if v, err := strconv.Atoi(os.Getenv(key)); err == nil && v > 0 {
		return v
	}
	return fallback
}
