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
package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"strconv"
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

	log.Printf("worker started, polling %s", api.baseURL)
	for ctx.Err() == nil {
		worked, err := runOnce(ctx, api)
		if err != nil {
			log.Printf("error: %v", err)
		}
		if !worked {
			select {
			case <-ctx.Done():
			case <-time.After(poll):
			}
		}
	}
	log.Print("worker stopped")
}

// runOnce takes at most one parse job. It returns true if there was a job.
func runOnce(ctx context.Context, api *API) (bool, error) {
	// Locating demos through CS2's Game Coordinator needs a logged-in Steam account and is
	// not built yet (see README). Until then, demos arrive by players uploading them.
	jobs, err := api.ParseJobs(ctx, 1)
	if err != nil || len(jobs) == 0 {
		return false, err
	}
	job := jobs[0]
	log.Printf("parsing match %s", job.MatchID)

	parsed, err := parseJob(ctx, api, job)
	if err != nil {
		log.Printf("match %s failed: %v", job.MatchID, err)
		_, expired := err.(expiredError)
		return true, api.ReportFailure(ctx, job.MatchID, err.Error(), expired)
	}
	result, err := api.ReportParsed(ctx, job.MatchID, parsed)
	if err != nil {
		return true, err
	}
	log.Printf("match %s done: %s", job.MatchID, result)
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
