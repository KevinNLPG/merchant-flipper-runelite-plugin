# Merchant Flipper RuneLite plugin

A **read-only sensor** RuneLite plugin for the Merchant Flipper desktop app. It watches your own
Grand Exchange offers inside the RuneLite client and reports changes to the desktop app over a
local WebSocket. It never clicks, types, places/modifies/cancels offers, or automates anything
in-game - see [PRIVACY.md](PRIVACY.md) for the full data-handling story.

## What it does

- Subscribes to RuneLite's `GrandExchangeOfferChanged` event.
- Deduplicates redundant re-fires (RuneLite can re-emit the same offer state for all 8 slots on
  login) using an in-memory `Map<slot, lastSeenState>` - only genuine changes are sent.
- Opens an outbound WebSocket **client** connection to `ws://<host>:<port>` (default
  `127.0.0.1:47100`) - the plugin never listens on/binds a socket itself.
- Sends `hello` with a pairing token you paste in from the desktop app; reconnects with
  exponential backoff (1s -> 2s -> 4s -> ... capped at 60s) on any drop; sends a `heartbeat` every
  15s while connected.
- If the desktop app rejects the pairing token, the plugin stops retrying with that token and
  posts a chat message asking you to re-pair, instead of hammering the socket forever.
- Buffers not-yet-acknowledged `offer_event` messages to a small local file so a desktop-app
  outage (or a RuneLite restart) never silently drops or duplicates an event; see "Buffering
  design" below.

## Wire protocol

Implements protocol version 1, a small JSON message protocol shared with the (separate, closed-source)
Merchant Flipper desktop app that this plugin talks to: `hello` / `hello_ack` / `heartbeat` /
`offer_event` / `event_ack` / `profile_announce`, all as single-line JSON, one message per
WebSocket text frame.

## Buffering design

Not-yet-acknowledged `offer_event`s are tracked individually (keyed by `eventId`) in an
insertion-ordered map and persisted to
`<RuneLite dir>/merchant-flipper/offer-event-buffer.ndjson` (newline-delimited JSON, one event per
line). Every append or acknowledge rewrites the whole file to a temp file and atomically renames
it over the original - so a crash mid-write never corrupts the buffer, and a plugin/RuneLite
restart reloads exactly the set of events that were still unacknowledged. On reconnect, the plugin
replays every still-buffered event (oldest first) and only drops an event once its matching
`event_ack` arrives - not on a simple "flush after the batch" basis. This is a deliberate choice:
it costs an O(n) file rewrite per event (n is small in practice - at most a handful of offers were
likely to change while the desktop app was unreachable), in exchange for never losing or
duplicating a single event across restarts.

## WebSocket library choice

The plugin uses `okhttp3`'s `WebSocket` support, which ships as a transitive dependency of
`net.runelite:client` itself - so this plugin declares no separate WebSocket dependency at all,
keeping its footprint to exactly what the host client already provides.

## Building

```
./gradlew build      # Linux/macOS
gradlew.bat build     # Windows
```

Requires JDK 17 (or any JDK the Gradle wrapper can find) - `build.gradle` targets Java 11
bytecode/API (`options.release.set(11)`) so the compiled plugin matches the language level RuneLite
plugins are normally built against.

## Running it standalone for development

RuneLite plugins are normally developed and debugged without the Plugin Hub, by launching a real
RuneLite client with just this one plugin force-loaded. Two ways to do that, both driven by
`src/test/java/com/merchantflipper/MerchantFlipperPluginTest.java` (a manual runner with a
`main()` method - **not** a JUnit test, so `./gradlew test` never executes it):

```
./gradlew run
```

This runs the `run` task defined in `build.gradle`, which launches
`MerchantFlipperPluginTest.main()` on the full test classpath (a real RuneLite client + this
plugin, force-loaded via `ExternalPluginManager.loadBuiltin`), with `--developer-mode --debug`.
Log in with a real OSRS account to exercise the plugin against live GE offers.

Alternatively, run/debug `MerchantFlipperPluginTest` directly from an IDE with the test source set
on the classpath.

## Running the tests

```
./gradlew test
```

All JUnit tests are pure-logic tests (dedupe/state-machine, buffer replay/ordering/durability,
backoff calculation, exact JSON (de)serialization against the protocol schema) - none of them
require a live RuneLite client or network connection.

## Pairing, from the user's perspective

1. Open the Merchant Flipper desktop app, go to **Settings > RuneLite**, and copy the pairing
   token shown there (it stays valid until you regenerate it in the desktop app).
2. In RuneLite, open the plugin's config panel (search "Merchant Flipper" in the plugin list) and
   paste the token into **Pairing token**. Leave **Desktop app host**/**Desktop app port** at their
   defaults (`127.0.0.1` / `47100`) unless you changed the desktop app's configured port.
3. Enable the plugin (or toggle it off/on if it's already enabled). You should see a
   "Connected to the Merchant Flipper desktop app." chat message once the handshake succeeds.
4. If you ever see a "Pairing rejected" chat message, go back to the desktop app, copy a fresh
   token, paste it into the plugin config, then toggle the plugin off and on again.

## What could not be verified in this environment

The full connect/hello/offer_event/reconnect flow against a *real* desktop-app WebSocket server,
and the `GrandExchangeOfferChanged` event against a *real*, logged-in OSRS game client, both
require infrastructure (a running desktop app instance, a logged-in OSRS account, a real RuneLite
client window) that isn't available in this build/test environment. All the pure logic that drives
that flow (dedupe, buffering/replay, backoff, exact JSON shape) is covered by the JUnit tests
instead.
