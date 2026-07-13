package com.merchantflipper.protocol;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Parses a raw JSON string received from the desktop app into one of the
 * known desktop -> plugin message types. Never throws; returns {@code null}
 * on any malformed input, and {@link Type#UNKNOWN} for a well-formed message
 * whose {@code type} field is not recognized (e.g. a future protocol
 * addition), so the caller can safely ignore it instead of crashing.
 */
public final class IncomingMessage
{
	public enum Type
	{
		HELLO_ACK,
		HEARTBEAT,
		EVENT_ACK,
		UNKNOWN
	}

	private final Type type;
	private final HelloAckMessage helloAck;
	private final HeartbeatMessage heartbeat;
	private final EventAckMessage eventAck;

	private IncomingMessage(Type type, HelloAckMessage helloAck, HeartbeatMessage heartbeat, EventAckMessage eventAck)
	{
		this.type = type;
		this.helloAck = helloAck;
		this.heartbeat = heartbeat;
		this.eventAck = eventAck;
	}

	public Type getType()
	{
		return type;
	}

	public HelloAckMessage getHelloAck()
	{
		return helloAck;
	}

	public HeartbeatMessage getHeartbeat()
	{
		return heartbeat;
	}

	public EventAckMessage getEventAck()
	{
		return eventAck;
	}

	public static IncomingMessage parse(String raw)
	{
		if (raw == null || raw.trim().isEmpty())
		{
			return null;
		}

		try
		{
			JsonObject obj = ProtocolCodec.gson().fromJson(raw, JsonObject.class);
			if (obj == null || !obj.has("type") || !obj.get("type").isJsonPrimitive())
			{
				return null;
			}

			String type = obj.get("type").getAsString();
			switch (type)
			{
				case "hello_ack":
					return new IncomingMessage(Type.HELLO_ACK,
						ProtocolCodec.gson().fromJson(obj, HelloAckMessage.class), null, null);
				case "heartbeat":
					return new IncomingMessage(Type.HEARTBEAT,
						null, ProtocolCodec.gson().fromJson(obj, HeartbeatMessage.class), null);
				case "event_ack":
					return new IncomingMessage(Type.EVENT_ACK,
						null, null, ProtocolCodec.gson().fromJson(obj, EventAckMessage.class));
				default:
					return new IncomingMessage(Type.UNKNOWN, null, null, null);
			}
		}
		catch (JsonSyntaxException e)
		{
			return null;
		}
	}
}
