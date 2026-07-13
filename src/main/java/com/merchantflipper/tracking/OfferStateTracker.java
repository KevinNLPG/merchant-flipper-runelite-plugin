package com.merchantflipper.tracking;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the last-seen {@link OfferSnapshot} per GE slot (0..7) in memory and
 * decides whether a newly observed snapshot represents an actual change worth
 * emitting.
 *
 * <p>This is the core idempotency guard required by the wire protocol: on
 * login RuneLite may re-fire {@code GrandExchangeOfferChanged} for all 8
 * slots even though nothing changed since the last time we observed them in
 * this same plugin session - those redundant re-fires must be suppressed.
 * The very first observation of a given slot in a session always compares
 * against "nothing seen yet" and is therefore always emitted; this is
 * intentional; see the plugin README for the rationale.
 */
public final class OfferStateTracker
{
	private final Map<Integer, OfferSnapshot> lastSeen = new ConcurrentHashMap<>();

	/**
	 * Records {@code snapshot} as the latest state for {@code slot}. Returns the
	 * snapshot wrapped in {@link Optional} if it differs from the previously
	 * recorded state for that slot (or if there was no previous state), or an
	 * empty {@link Optional} if it's an exact duplicate that must be suppressed.
	 */
	public synchronized Optional<OfferSnapshot> update(int slot, OfferSnapshot snapshot)
	{
		OfferSnapshot previous = lastSeen.get(slot);
		if (snapshot.equals(previous))
		{
			return Optional.empty();
		}
		lastSeen.put(slot, snapshot);
		return Optional.of(snapshot);
	}

	public OfferSnapshot getLastSeen(int slot)
	{
		return lastSeen.get(slot);
	}

	public void reset()
	{
		lastSeen.clear();
	}
}
