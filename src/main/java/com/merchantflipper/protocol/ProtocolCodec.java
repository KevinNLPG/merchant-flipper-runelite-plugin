package com.merchantflipper.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Single shared Gson instance and JSON encode helper for outgoing messages.
 * Uses plain field-name serialization (no naming policy) so the wire JSON
 * matches {@code packages/shared/src/protocol/v1.ts} byte-for-byte.
 */
public final class ProtocolCodec
{
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

	private ProtocolCodec()
	{
	}

	public static Gson gson()
	{
		return GSON;
	}

	/** Serializes any of the plugin -> desktop message POJOs to a single-line JSON string. */
	public static String toJson(Object message)
	{
		return GSON.toJson(message);
	}
}
