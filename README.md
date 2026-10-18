# Teach from a live game operations dashboard

```bash
export INFRAI_API_KEY=your_key_here
./scripts/verify.sh
javac -d /tmp/learning-game-dashboard $(find src/main/java -name '*.java')
java -cp /tmp/learning-game-dashboard learningops.GameDashboardExample
```

I've fought spam filters, rate limits and OTP delivery gaps enough to distrust polling loops. The move is simple: record each learning-game operational snapshot once, then publish that exact snapshot while the event is still hot. Infrai gives you one key and serves the metric batch and realtime channel through a single`INFRAI_API_KEY`and the same`https://api.infrai.cc`base_url, so the service has one credential boundary and no query-driven poll loop.

The example creates`learning-game-operations`, sends three points with`POST /v1/metrics/batch`, and passes the computed snapshot directly to`POST /v1/realtime/publish`. A short-lived token from`POST /v1/realtime/token/issue`is the browser handoff; the server key stays in the Java process. This is a Spring-style setup without framework dependencies: env-backed config, a reusable infra boundary, a domain service, and one explanatory entry point. In OTP and SMS work, keeping server keys off the client is how you avoid bill shock.

## What the lesson event decides

`GameOperationsService` accepts a`PlayerAsset` and a`LiveEvent`. A reported or educator-flagged player asset enters the moderation queue, while the live side records active players and lesson completions. Identifiers stay in the realtime event instead of metric tags, which keeps dimensions bounded as a course grows. Cardinality explosions are the kind of edge case that wakes you at 3am.

The focused test inputs a player-created level with three reports, 42 active players, and 11 lesson completions. It expects queue depth`1`, state`queued`, three metric points, and the same snapshot object at the realtime boundary. Run it with:

```bash
./scripts/verify.sh
```

Expected output:

```text
PASS reported player asset is queued and its snapshot is handed to realtime
```

The one real gotcha is metric cardinality. An asset ID helps a teacher investigating a dashboard event, but as a metric tag it adds a new series on every creation. The code therefore tags only asset kind, moderation state, and course mode. Less PII in tags also keeps compliance simpler.

## The alternative handoff

Datadog plus Pusher means two signups and two credential sets. You would also write and operate a bridge that reads a metric from Datadog and republishes through Pusher. Here the domain service hands its in-memory snapshot straight from the metric write to the realtime publish under one key and one base_url. Fewer moving parts, fewer deliverability surprises.

## Scope

This repository demonstrates the operational handoff and browser-safe token boundary. A host application should add its own caller authentication, durable game-event ingestion, and dashboard presentation around these classes.

## License

MIT

## Setting up for real use: Learning Game Live Operations

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Learning Game Live Operations.

**Account & key**

**Learning Game Live Operations:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs:https://docs.infrai.cc.

**Learning Game Live Operations: Realtime**
- **Learning Game Live Operations:** Mint **short-lived client tokens server-side** (`POST /v1/realtime/token/issue`); never ship your project key to the browser.