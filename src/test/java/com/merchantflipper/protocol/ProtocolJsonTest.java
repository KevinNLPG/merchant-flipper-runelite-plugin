package com.merchantflipper.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import org.junit.Test;

/**
 * Verifies our POJOs serialize/deserialize to exactly the wire schema defined in
 * {@code packages/shared/src/protocol/v1.ts} - field names, nesting, and value shapes.
 */
public class ProtocolJsonTest
{
	@Test
	public void helloMessageMatchesClientHelloSchema()
	{
		HelloMessage hello = new HelloMessage(Protocol.VERSION, "0.1.0", "abcdefghijklmnopqrst");
		JsonObject json = ProtocolCodec.gson().toJsonTree(hello).getAsJsonObject();

		assertEquals("hello", json.get("type").getAsString());
		assertEquals(1, json.get("protocolVersion").getAsInt());
		assertEquals("0.1.0", json.get("pluginVersion").getAsString());
		assertEquals("abcdefghijklmnopqrst", json.get("pairingToken").getAsString());
		assertEquals(4, json.size());
	}

	@Test
	public void heartbeatMessageMatchesHeartbeatSchema()
	{
		HeartbeatMessage heartbeat = new HeartbeatMessage(1_720_000_000_000L);
		JsonObject json = ProtocolCodec.gson().toJsonTree(heartbeat).getAsJsonObject();

		assertEquals("heartbeat", json.get("type").getAsString());
		assertEquals(1_720_000_000_000L, json.get("timestamp").getAsLong());
		assertEquals(2, json.size());
	}

	@Test
	public void profileAnnounceMessageMatchesProfileAnnounceSchema()
	{
		ProfileAnnounceMessage announce = new ProfileAnnounceMessage("profile-hash", 1_720_000_000_000L);
		JsonObject json = ProtocolCodec.gson().toJsonTree(announce).getAsJsonObject();

		assertEquals("profile_announce", json.get("type").getAsString());
		assertEquals("profile-hash", json.get("profileId").getAsString());
		assertEquals(1_720_000_000_000L, json.get("timestamp").getAsLong());
		assertEquals(3, json.size());
	}

	@Test
	public void offerEventMessageMatchesOfferEventMessageSchema()
	{
		RawOfferEvent event = new RawOfferEvent(
			1,
			"11111111-1111-1111-1111-111111111111",
			1_720_000_000_000L,
			"profile-hash",
			0,
			4151,
			"BUYING",
			10,
			3,
			1500,
			4500);
		OfferEventMessage message = new OfferEventMessage(event);

		JsonObject json = ProtocolCodec.gson().toJsonTree(message).getAsJsonObject();
		assertEquals("offer_event", json.get("type").getAsString());

		JsonObject eventJson = json.getAsJsonObject("event");
		assertEquals(1, eventJson.get("protocolVersion").getAsInt());
		assertEquals("11111111-1111-1111-1111-111111111111", eventJson.get("eventId").getAsString());
		assertEquals(1_720_000_000_000L, eventJson.get("timestamp").getAsLong());
		assertEquals("profile-hash", eventJson.get("profileId").getAsString());
		assertEquals(0, eventJson.get("slot").getAsInt());
		assertEquals(4151, eventJson.get("itemId").getAsInt());
		assertEquals("BUYING", eventJson.get("state").getAsString());
		assertEquals(10, eventJson.get("totalQuantity").getAsInt());
		assertEquals(3, eventJson.get("filledQuantity").getAsInt());
		assertEquals(1500, eventJson.get("offerPrice").getAsInt());
		assertEquals(4500, eventJson.get("totalSpentOrReceived").getAsInt());
		assertEquals(11, eventJson.size());
	}

	@Test
	public void parsesAcceptedHelloAck()
	{
		String raw = "{\"type\":\"hello_ack\",\"protocolVersion\":1,\"accepted\":true}";
		IncomingMessage message = IncomingMessage.parse(raw);

		assertNotNull(message);
		assertEquals(IncomingMessage.Type.HELLO_ACK, message.getType());
		assertTrue(message.getHelloAck().isAccepted());
		assertNull(message.getHelloAck().getReason());
	}

	@Test
	public void parsesRejectedHelloAckWithReason()
	{
		String raw = "{\"type\":\"hello_ack\",\"protocolVersion\":1,\"accepted\":false,\"reason\":\"unknown token\"}";
		IncomingMessage message = IncomingMessage.parse(raw);

		assertNotNull(message);
		assertFalse(message.getHelloAck().isAccepted());
		assertEquals("unknown token", message.getHelloAck().getReason());
	}

	@Test
	public void parsesEventAck()
	{
		String raw = "{\"type\":\"event_ack\",\"eventId\":\"11111111-1111-1111-1111-111111111111\"}";
		IncomingMessage message = IncomingMessage.parse(raw);

		assertNotNull(message);
		assertEquals(IncomingMessage.Type.EVENT_ACK, message.getType());
		assertEquals("11111111-1111-1111-1111-111111111111", message.getEventAck().getEventId());
	}

	@Test
	public void parsesServerHeartbeat()
	{
		String raw = "{\"type\":\"heartbeat\",\"timestamp\":1720000000000}";
		IncomingMessage message = IncomingMessage.parse(raw);

		assertNotNull(message);
		assertEquals(IncomingMessage.Type.HEARTBEAT, message.getType());
		assertEquals(1_720_000_000_000L, message.getHeartbeat().getTimestamp());
	}

	@Test
	public void malformedOrEmptyInputReturnsNullRatherThanThrowing()
	{
		assertNull(IncomingMessage.parse("not json"));
		assertNull(IncomingMessage.parse(""));
		assertNull(IncomingMessage.parse(null));
		assertNull(IncomingMessage.parse("{\"noTypeField\":true}"));
	}

	@Test
	public void unrecognizedTypeParsesAsUnknownRatherThanNull()
	{
		IncomingMessage message = IncomingMessage.parse("{\"type\":\"something_from_a_future_protocol_version\"}");
		assertNotNull(message);
		assertEquals(IncomingMessage.Type.UNKNOWN, message.getType());
	}
}
