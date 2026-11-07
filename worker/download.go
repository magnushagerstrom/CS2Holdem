package main

import (
	"bufio"
	"compress/bzip2"
	"context"
	"fmt"
	"io"
	"net/http"
)

// expiredError means Valve no longer has the demo. Retrying will not help.
type expiredError struct{ status string }

func (e expiredError) Error() string { return "demo is gone from Valve's servers (" + e.status + ")" }

// downloadDemo returns the demo as a stream, uncompressed.
func downloadDemo(ctx context.Context, api *API, job ParseJob) (io.ReadCloser, error) {
	var body io.ReadCloser
	if job.DemoURL == "" {
		var err error
		if body, err = api.OpenUploadedDemo(ctx, job.MatchID); err != nil {
			return nil, err
		}
	} else {
		req, err := http.NewRequestWithContext(ctx, http.MethodGet, job.DemoURL, nil)
		if err != nil {
			return nil, err
		}
		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			return nil, err
		}
		if resp.StatusCode == http.StatusNotFound || resp.StatusCode == http.StatusGone {
			resp.Body.Close()
			return nil, expiredError{resp.Status}
		}
		if resp.StatusCode != http.StatusOK {
			resp.Body.Close()
			return nil, fmt.Errorf("demo download answered %s", resp.Status)
		}
		body = resp.Body
	}
	return decompress(body)
}

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
