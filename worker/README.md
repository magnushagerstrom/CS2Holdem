# CS2Holdem demo worker (Go)

Reads CS2 demo files and sends every kill to the Java server, which turns them into cards.

> **Status: working.** Tested against real CS2 Premier demos fetched through the Game Coordinator.

## Why Go, and why a separate program?

- The best CS2 demo parser, [demoinfocs-golang](https://github.com/markus-wa/demoinfocs-golang),
  is a Go library. There is no Java equivalent that keeps up with CS2 updates.
- Reading a 100 MB demo takes seconds of CPU. Doing it in a separate program means the web
  server stays fast, and on AWS you can run more workers when there are more demos.

## How it works

1. Every few seconds: `GET /internal/jobs/parse` ("any demos to read?")
2. Download the demo in full to a temporary file, from Valve's link or from our server
   (uploaded demos). A broken download is resumed where it stopped, and the size is checked
   against what the server promised before anything is read. Then unpack `.bz2`
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

## Where the demos come from

The Java server's `locator` module logs in a bot Steam account and asks CS2's Game Coordinator
for each match's download link. The worker only sees the finished job: a match id and a link to
Valve's replay server. Players can also upload a demo themselves (`POST /api/matches/{id}/demo`),
for example when Valve's copy has expired; the job then has no link and the worker fetches the
file from our own server instead.
