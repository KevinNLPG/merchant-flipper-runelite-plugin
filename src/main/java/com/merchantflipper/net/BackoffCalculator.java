package com.merchantflipper.net;

/**
 * Exponential backoff with a cap, e.g. 1s, 2s, 4s, 8s, ... capped at ~60s.
 * Not thread-safe by itself; callers that touch it from multiple threads
 * (e.g. a reconnect scheduler and a manual "reconfigure" call) must
 * externally synchronize, as {@link ConnectionManager} does.
 */
public final class BackoffCalculator
{
	private static final long DEFAULT_INITIAL_MILLIS = 1_000L;
	private static final long DEFAULT_MAX_MILLIS = 60_000L;

	/** Cap on the internal attempt counter so long uptimes never risk overflow in the doubling math. */
	private static final int MAX_ATTEMPT_EXPONENT = 32;

	private final long initialMillis;
	private final long maxMillis;
	private int attempt = 0;

	public BackoffCalculator()
	{
		this(DEFAULT_INITIAL_MILLIS, DEFAULT_MAX_MILLIS);
	}

	public BackoffCalculator(long initialMillis, long maxMillis)
	{
		this.initialMillis = initialMillis;
		this.maxMillis = maxMillis;
	}

	/** Returns the next delay to wait before reconnecting, and advances the internal attempt counter. */
	public long nextDelayMillis()
	{
		long delay = (long) (initialMillis * Math.pow(2, attempt));
		if (attempt < MAX_ATTEMPT_EXPONENT)
		{
			attempt++;
		}
		return Math.min(delay, maxMillis);
	}

	/** Call after a successful connection so the next disconnect starts backing off from the initial delay again. */
	public void reset()
	{
		attempt = 0;
	}
}
