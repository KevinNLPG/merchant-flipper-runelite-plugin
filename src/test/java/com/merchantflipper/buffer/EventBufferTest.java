package com.merchantflipper.buffer;

import static org.junit.Assert.assertEquals;

import com.merchantflipper.protocol.RawOfferEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EventBufferTest
{
	private Path tempDir;
	private Path bufferFile;

	@Before
	public void setUp() throws IOException
	{
		tempDir = Files.createTempDirectory("merchant-flipper-buffer-test");
		bufferFile = tempDir.resolve("events.ndjson");
	}

	@After
	public void tearDown() throws IOException
	{
		try (Stream<Path> walk = Files.walk(tempDir))
		{
			walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
		}
	}

	private RawOfferEvent event(String id, int slot)
	{
		return new RawOfferEvent(1, id, 1_000L, "profile-1", slot, 4151, "BUYING", 10, 0, 1500, 0);
	}

	@Test
	public void appendPersistsAndReturnsEventsInInsertionOrder() throws IOException
	{
		EventBuffer buffer = new EventBuffer(bufferFile);
		buffer.append(event("a", 0));
		buffer.append(event("b", 1));
		buffer.append(event("c", 2));

		List<RawOfferEvent> pending = buffer.pendingInOrder();
		assertEquals(3, pending.size());
		assertEquals("a", pending.get(0).getEventId());
		assertEquals("b", pending.get(1).getEventId());
		assertEquals("c", pending.get(2).getEventId());
	}

	@Test
	public void acknowledgeRemovesOnlyThatEventAndPreservesOrder() throws IOException
	{
		EventBuffer buffer = new EventBuffer(bufferFile);
		buffer.append(event("a", 0));
		buffer.append(event("b", 1));
		buffer.append(event("c", 2));

		buffer.acknowledge("b");

		List<RawOfferEvent> pending = buffer.pendingInOrder();
		assertEquals(2, pending.size());
		assertEquals("a", pending.get(0).getEventId());
		assertEquals("c", pending.get(1).getEventId());
	}

	@Test
	public void survivesARestartByReloadingUnacknowledgedEventsFromDisk() throws IOException
	{
		EventBuffer buffer = new EventBuffer(bufferFile);
		buffer.append(event("a", 0));
		buffer.append(event("b", 1));
		buffer.append(event("c", 2));
		buffer.acknowledge("a");

		// Simulate a plugin/JVM restart: a brand new EventBuffer pointed at the same file.
		EventBuffer reloaded = new EventBuffer(bufferFile);
		List<RawOfferEvent> pending = reloaded.pendingInOrder();
		assertEquals(2, pending.size());
		assertEquals("b", pending.get(0).getEventId());
		assertEquals("c", pending.get(1).getEventId());
	}

	@Test
	public void acknowledgingAnUnknownEventIdIsANoop() throws IOException
	{
		EventBuffer buffer = new EventBuffer(bufferFile);
		buffer.append(event("a", 0));
		buffer.acknowledge("does-not-exist");
		assertEquals(1, buffer.pendingInOrder().size());
	}

	@Test
	public void reappendingTheSameEventIdDoesNotDuplicateIt() throws IOException
	{
		EventBuffer buffer = new EventBuffer(bufferFile);
		buffer.append(event("a", 0));
		buffer.append(event("a", 0));
		assertEquals(1, buffer.pendingInOrder().size());
	}

	@Test
	public void acknowledgingEverythingLeavesAnEmptyBufferOnReload() throws IOException
	{
		EventBuffer buffer = new EventBuffer(bufferFile);
		buffer.append(event("a", 0));
		buffer.append(event("b", 1));
		buffer.acknowledge("a");
		buffer.acknowledge("b");

		EventBuffer reloaded = new EventBuffer(bufferFile);
		assertEquals(0, reloaded.pendingInOrder().size());
	}
}
