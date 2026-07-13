package com.merchantflipper.protocol;

/**
 * Shared constants for the local-only WebSocket protocol between this plugin
 * and the Merchant Flipper desktop app. Must stay in lockstep with
 * {@code packages/shared/src/protocol/v1.ts} in the main repo.
 */
public final class Protocol
{
	/** Current wire protocol version. Bump only in lockstep with the desktop app. */
	public static final int VERSION = 1;

	private Protocol()
	{
	}
}
