# Privacy

This plugin is a read-only sensor. It observes your own Grand Exchange offers inside the RuneLite
client and reports changes to the Merchant Flipper desktop app. Specifically:

- **Where data goes**: only to `127.0.0.1` (configurable port, default `47100`) - i.e. only to the
  Merchant Flipper desktop app running on the *same machine*. The plugin opens an outbound
  WebSocket **client** connection to that address; it never listens on/binds a socket itself, and
  it never connects to any remote host. There is no analytics, telemetry, or third-party endpoint
  of any kind.
- **What data is sent**: for each real change to one of your GE offers - item id, GE offer state
  (`EMPTY`/`BUYING`/`BOUGHT`/`SELLING`/`SOLD`/`CANCELLED_BUY`/`CANCELLED_SELL`), total quantity,
  filled quantity, offer price, and cumulative gp spent/received - plus a per-account identifier
  derived by running `client.getAccountHash()` through SHA-256. That identifier is a one-way
  digest: it lets the desktop app tell "flips from account A" apart from "flips from account B"
  across sessions, without ever seeing or being able to recover your display name (RSN), account
  hash, login, password, session token, or any other credential.
- **What is never read or sent**: your RSN/display name, your password or session/login tokens,
  chat messages, inventory, bank contents, or anything outside of your own Grand Exchange offer
  slots.
- **No automation**: this plugin never clicks, types, moves the mouse, or calls any API that
  places, modifies, or cancels a GE offer. It does not use `java.awt.Robot`, input simulation,
  reflection-based game-state mutation, or native injection of any kind. It only subscribes to
  RuneLite's `GrandExchangeOfferChanged` event and reads offer state through the standard,
  documented `net.runelite.api.GrandExchangeOffer` getters.
- **Local buffering**: if the desktop app is unreachable, not-yet-acknowledged offer events are
  queued in a local file under `<RuneLite dir>/merchant-flipper/offer-event-buffer.ndjson` (your
  own machine's RuneLite data directory) until they can be delivered and acknowledged. This file
  never leaves your machine and contains only the same GE offer fields described above.
- **Pairing token**: the token you paste into the plugin's config is used solely to authenticate
  this plugin instance to your own local desktop app instance over that same localhost socket. It
  is not sent anywhere else.
