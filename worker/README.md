# CS2Holdem demo worker (Go)

Reads CS2 demo files and sends every kill to the Java server, which turns them into cards.

> **Status: written but not yet run.** It was written without network access, so it has never
> been compiled against the library. Expect small fixes the first time (`go build` will point
> at them). The Java side it talks to is tested.

## Why Go, and why a separate program?

- The best CS2 demo parser, [demoinfocs-golang](https://github.com/markus-wa/demoinfocs-golang),
  is a Go library. There is no Java equivalent that keeps up with CS2 updates.
- Reading a 100 MB demo takes seconds of CPU. Doing it in a separate program means the web
  server stays fast, and on AWS you can run more workers when there are more demos.

## How it works

1. Every few seconds: `GET /internal/jobs/parse` ("any demos to read?")
2. Download the demo, from Valve's link or from our server (uploaded demos), and unpack `.bz2`
3. Read it with demoinfocs: kills, damage, rounds, positions
4. `POST /internal/matches/{id}/parsed` with the result, or `/failed` with the error

Several workers can run at once: the server hands each match to only one of them
(`FOR UPDATE SKIP LOCKED`, see `PipelineMatchRepository.java`).

## Run it

```bash
cd worker
go mod tidy                     # downloads demoinfocs, creates go.sum
WORKER_TOKEN=<same value as in ../.env> go run .
```

On Windows PowerShell: `$env:WORKER_TOKEN="..."; go run .`

## Not built yet

**Locating demos automatically.** To get Valve's download link for a match, a program has to
log in to Steam with a real account and ask CS2's Game Coordinator (`/internal/jobs/locate`
exists on the server for this). Until that is built, players upload their demos themselves:
`POST /api/matches/{id}/demo`.
