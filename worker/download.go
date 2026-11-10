package main

import (
	"bufio"
	"compress/bzip2"
	"context"
	"errors"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
)

const (
	// Valve's replay servers drop connections often, sometimes every minute. We continue where we
	// were, and only give up after this many attempts in a row that brought no new bytes at all.
	maxFailuresWithoutProgress = 6
	retryPause          = 5 * time.Second
)

// expiredError means Valve no longer has the demo. Retrying will not help.
type expiredError struct{ status string }

func (e expiredError) Error() string { return "demo is gone from Valve's servers (" + e.status + ")" }

// downloadDemo returns the demo, uncompressed, ready to be read from the start.
//
// Valve's demos are first saved to a temporary file, in full, and only then read. Reading while
// downloading looked simpler, but it hid a dangerous failure: when Valve closed the connection
// early, the parser saw a demo that simply ended, and reported half a match as a finished one.
// With a file we know the exact size the server promised and can check that we got all of it.
func downloadDemo(ctx context.Context, api *API, job ParseJob) (io.ReadCloser, error) {
	if job.DemoURL == "" {
		body, err := api.OpenUploadedDemo(ctx, job.MatchID)
		if err != nil {
			return nil, err
		}
		return decompress(body)
	}
	file, err := downloadToFile(ctx, job.DemoURL, job.MatchID)
	if err != nil {
		return nil, err
	}
	return decompress(file)
}

// tempFile is an *os.File that deletes itself when closed.
type tempFile struct{ *os.File }

func (t tempFile) Close() error {
	err := t.File.Close()
	os.Remove(t.Name())
	return err
}

// downloadToFile fetches url into a temporary file. If the connection breaks, it asks the server
// for only the missing part (an HTTP Range request) instead of starting over.
func downloadToFile(ctx context.Context, url, matchID string) (io.ReadCloser, error) {
	f, err := os.CreateTemp("", "cs2holdem-demo-*.dem.bz2")
	if err != nil {
		return nil, err
	}
	file := tempFile{f}
	started := time.Now()
	var have, total int64 = 0, -1

	failures := 0
	for {
		before := have
		have, total, err = downloadPart(ctx, url, file.File, have, total)
		if err == nil {
			break
		}
		if have > before {
			failures = 0 // it broke, but we got further: not a failure that counts
		}
		failures++
		var expired expiredError
		if errors.As(err, &expired) || ctx.Err() != nil || failures == maxFailuresWithoutProgress {
			file.Close()
			return nil, err
		}
		log.Printf("match %s: download interrupted after %s (%v), continuing in %s",
			matchID, megabytes(have), err, retryPause)
		select {
		case <-ctx.Done():
			file.Close()
			return nil, ctx.Err()
		case <-time.After(retryPause):
		}
	}

	took := time.Since(started)
	log.Printf("match %s: downloaded %s in %s (%.1f MB/s)",
		matchID, megabytes(have), took.Round(time.Second), float64(have)/1e6/took.Seconds())
	if _, err := file.Seek(0, io.SeekStart); err != nil {
		file.Close()
		return nil, err
	}
	return file, nil
}

// downloadPart downloads from byte `have` onwards and appends to file.
// It returns the new byte count and the full size the server reported (-1 if unknown).
func downloadPart(ctx context.Context, url string, file *os.File, have, total int64) (int64, int64, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
	if err != nil {
		return have, total, err
	}
	if have > 0 {
		req.Header.Set("Range", fmt.Sprintf("bytes=%d-", have))
	}
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return have, total, err
	}
	defer resp.Body.Close()

	switch {
	case resp.StatusCode == http.StatusNotFound || resp.StatusCode == http.StatusGone:
		return have, total, expiredError{resp.Status}
	case resp.StatusCode == http.StatusPartialContent:
		// "bytes 1000-49999/50000": the number after the slash is the full size
		if i := strings.LastIndex(resp.Header.Get("Content-Range"), "/"); i >= 0 {
			if n, err := strconv.ParseInt(resp.Header.Get("Content-Range")[i+1:], 10, 64); err == nil {
				total = n
			}
		}
	case resp.StatusCode == http.StatusOK:
		// The whole file again (first try, or a server that ignores Range): start from zero
		if err := file.Truncate(0); err != nil {
			return have, total, err
		}
		if _, err := file.Seek(0, io.SeekStart); err != nil {
			return have, total, err
		}
		have = 0
		total = resp.ContentLength
	default:
		return have, total, fmt.Errorf("demo download answered %s", resp.Status)
	}

	written, err := io.Copy(file, resp.Body)
	have += written
	if err != nil {
		return have, total, err
	}
	if total >= 0 && have < total {
		// The server ended the response early but politely: no error, just too few bytes
		return have, total, fmt.Errorf("got %s of %s", megabytes(have), megabytes(total))
	}
	return have, total, nil
}

func megabytes(n int64) string { return fmt.Sprintf("%.1f MB", float64(n)/1e6) }

// decompress unpacks bzip2 (how Valve serves demos) and passes plain .dem files through.
func decompress(body io.ReadCloser) (io.ReadCloser, error) {
	buffered := bufio.NewReaderSize(body, 64*1024)
	head, err := buffered.Peek(3)
	if err != nil {
		body.Close()
		return nil, fmt.Errorf("demo is empty or unreadable: %w", err)
	}
	var reader io.Reader = buffered
	if string(head) == "BZh" {
		reader = bzip2.NewReader(buffered)
	}
	return struct {
		io.Reader
		io.Closer
	}{reader, body}, nil
}
