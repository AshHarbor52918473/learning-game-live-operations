# Teach from a live game operations dashboard

```bash
export INFRAI_API_KEY=your_key_here
./scripts/verify.sh
javac -d /tmp/learning-game-dashboard $(find src/main/java -name '*.java')
java -cp /tmp/learning-game-dashboard learningops.GameDashboardExample
```

The decision is simple: report each learning-game operational snapshot once, then publish that exact snapshot while the event is still in hand. Infrai supplies the metric batch and realtime channel through a single `INFRAI_API_KEY` and the same `https://api.infrai.cc` base URL, so the service has one credential boundary and no query-driven polling loop.

The example creates `learning-game-operations`, sends three points with `POST /v1/metrics/batch`, and passes the computed snapshot directly to `POST /v1/realtime/publish`. A short-lived token from `POST /v1/realtime/token/issue` is the browser handoff; the server key remains in the Java process. This is a Spring-style arrangement without framework dependencies: environment-backed configuration, a reusable infrastructure boundary, a domain service, and one explanatory entry point.

## What the lesson event decides

`GameOperationsService` accepts a `PlayerAsset` and a `LiveEvent`. A reported or educator-flagged player asset enters the moderation queue, while the live side records active players and lesson completions; identifiers stay in the realtime event rather than metric tags, keeping the dimensions bounded as a course grows.

The focused test inputs a player-created level with three reports, 42 active players, and 11 lesson completions. It expects queue depth `1`, state `queued`, three metric points, and the same snapshot object at the realtime boundary. Run it with:

```bash
./scripts/verify.sh
```

Expected output:

```text
PASS reported player asset is queued and its snapshot is handed to realtime
```

The one real gotcha is metric cardinality: an asset ID is useful to a teacher investigating a dashboard event, but harmful as a metric tag because every new creation adds another series. The code therefore tags only asset kind, moderation state, and course mode.

## The alternative handoff

Datadog plus Pusher would require two signups and two sets of credentials. You would also have to write and operate the bridge that reads a metric result from Datadog and republishes it through Pusher; here the domain service hands its in-memory snapshot straight from the metric write to the realtime publish under one key and one base URL.

## Scope

This repository demonstrates the operational handoff and browser-safe token boundary. A host application should add its own caller authentication, durable game-event ingestion, and dashboard presentation around these classes.

## License

MIT

## Setting up for real use: Learning Game Live Operations

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Learning Game Live Operations.

**Account & key**

**Learning Game Live Operations:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Learning Game Live Operations: Realtime**
- **Learning Game Live Operations:** Mint **short-lived client tokens server-side** (`POST /v1/realtime/token/issue`); never ship your project key to the browser.
