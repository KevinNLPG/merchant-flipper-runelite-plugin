package com.merchantflipper.buffer;

import com.google.gson.JsonSyntaxException;
import com.merchantflipper.protocol.ProtocolCodec;
import com.merchantflipper.protocol.RawOfferEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Local, durable, at-least-once buffer of not-yet-acknowledged {@code offer_event}s.
 *
 * <p>Design choice (documented per the spec's "pick one and document it"
 * option): events are tracked individually, keyed by {@code eventId}, in a
 * {@link LinkedHashMap} that preserves insertion order. Every mutation
 * (append or acknowledge) rewrites the backing newline-delimited-JSON file in
 * full to a temp file and atomically renames it over the original, so a
 * crash or forced shutdown mid-write never leaves a half-written or corrupt
 * file behind, and a plugin/RuneLite restart reloads exactly the set of
 * events that were still unacknowledged - never silently losing or
 * duplicating one. This is simpler and safer than in-place append/delete on
 * a shared file, at the cost of an O(n) rewrite per mutation; n is bounded by
 * "how many offer events happened while the desktop app was unreachable",
 * which is small in practice (at most a handful per GE slot).
 */
public final class EventBuffer
{
	private final Path filePath;
	private final Map<String, RawOfferEvent> pending = new LinkedHashMap<>();

	public EventBuffer(Path filePath) throws IOException
	{
		this.filePath = filePath;
		Path parent = filePath.getParent();
		if (parent != null)
		{
			Files.createDirectories(parent);
		}
		load();
	}

	private void load() throws IOException
	{
		if (!Files.exists(filePath))
		{
			return;
		}

		for (String line : Files.readAllLines(filePath, StandardCharsets.UTF_8))
		{
			if (line.trim().isEmpty())
			{
				continue;
			}
			try
			{
				RawOfferEvent event = ProtocolCodec.gson().fromJson(line, RawOfferEvent.class);
				if (event != null && event.getEventId() != null)
				{
					pending.put(event.getEventId(), event);
				}
			}
			catch (JsonSyntaxException e)
			{
				// Skip a corrupt line rather than failing plugin startup entirely.
			}
		}
	}

	/** Adds (or replaces, if the same eventId is re-appended) a pending event and persists immediately. */
	public synchronized void append(RawOfferEvent event) throws IOException
	{
		pending.put(event.getEventId(), event);
		persist();
	}

	/** Drops an event once its {@code event_ack} has arrived. No-op if the id is unknown. */
	public synchronized void acknowledge(String eventId) throws IOException
	{
		if (pending.remove(eventId) != null)
		{
			persist();
		}
	}

	/** All still-unacknowledged events, oldest first. */
	public synchronized List<RawOfferEvent> pendingInOrder()
	{
		return new ArrayList<>(pending.values());
	}

	public synchronized int size()
	{
		return pending.size();
	}

	private void persist() throws IOException
	{
		Path tmp = filePath.resolveSibling(filePath.getFileName().toString() + ".tmp");
		try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8))
		{
			for (RawOfferEvent event : pending.values())
			{
				writer.write(ProtocolCodec.toJson(event));
				writer.newLine();
			}
		}
		Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}
}
