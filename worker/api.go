package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"
)

// LocateJob and ParseJob mirror the Java records with the same names.
type LocateJob struct {
	MatchID       string `json:"matchId"`
	ShareCode     string `json:"shareCode"`
	ReservationID string `json:"reservationId"`
	TVPort        int    `json:"tvPort"`
}

type ParseJob struct {
	MatchID string `json:"matchId"`
	DemoURL string `json:"demoUrl"` // empty: the demo was uploaded, fetch it from the server
}

// API talks to the /internal endpoints of the Java server.
type API struct {
	baseURL string
	token   string
	http    *http.Client
}

func NewAPI(baseURL, token string) *API {
	return &API{baseURL: baseURL, token: token, http: &http.Client{Timeout: 2 * time.Minute}}
}

func (a *API) ParseJobs(ctx context.Context, limit int) ([]ParseJob, error) {
	var jobs []ParseJob
	err := a.call(ctx, http.MethodGet, fmt.Sprintf("/internal/jobs/parse?limit=%d", limit), nil, &jobs)
	return jobs, err
}

func (a *API) ReportParsed(ctx context.Context, matchID string, parsed *ParsedMatch) (string, error) {
	var result map[string]any
	err := a.call(ctx, http.MethodPost, "/internal/matches/"+matchID+"/parsed", parsed, &result)
	return fmt.Sprint(result), err
}

func (a *API) ReportFailure(ctx context.Context, matchID, message string, expired bool) error {
	body := map[string]any{"error": message, "expired": expired}
	return a.call(ctx, http.MethodPost, "/internal/matches/"+matchID+"/failed", body, nil)
}

// OpenUploadedDemo streams a demo a player uploaded. The caller closes it.
func (a *API) OpenUploadedDemo(ctx context.Context, matchID string) (io.ReadCloser, error) {
	req, err := a.request(ctx, http.MethodGet, "/internal/matches/"+matchID+"/demo", nil)
	if err != nil {
		return nil, err
	}
	// No timeout on the client used for big downloads: the context decides
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return nil, err
	}
	if resp.StatusCode != http.StatusOK {
		resp.Body.Close()
		return nil, fmt.Errorf("server answered %s", resp.Status)
	}
	return resp.Body, nil
}

func (a *API) call(ctx context.Context, method, path string, body any, out any) error {
	var payload io.Reader
	if body != nil {
		data, err := json.Marshal(body)
		if err != nil {
			return err
		}
		payload = bytes.NewReader(data)
	}
	req, err := a.request(ctx, method, path, payload)
	if err != nil {
		return err
	}
	resp, err := a.http.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 300 {
		msg, _ := io.ReadAll(io.LimitReader(resp.Body, 2000))
		return fmt.Errorf("%s %s: %s %s", method, path, resp.Status, msg)
	}
	if out != nil && resp.StatusCode != http.StatusNoContent {
		return json.NewDecoder(resp.Body).Decode(out)
	}
	return nil
}

func (a *API) request(ctx context.Context, method, path string, body io.Reader) (*http.Request, error) {
	req, err := http.NewRequestWithContext(ctx, method, a.baseURL+path, body)
	if err != nil {
		return nil, err
	}
	req.Header.Set("X-Worker-Token", a.token)
	if body != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	return req, nil
}
