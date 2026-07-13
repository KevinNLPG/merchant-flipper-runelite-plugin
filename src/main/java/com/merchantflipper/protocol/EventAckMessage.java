package com.merchantflipper.protocol;

/**
 * desktop -> plugin, sent once an {@code offer_event} has been durably persisted; the plugin
 * may then drop that eventId from its local retry buffer. Only ever received, but a
 * constructor is provided for convenience in tests.
 */
public final class EventAckMessage
{
	private String type;
	private String eventId;

	/** No-arg constructor used by Gson during deserialization. */
	public EventAckMessage()
	{
	}

	public EventAckMessage(String eventId)
	{
		this.type = "event_ack";
		this.eventId = eventId;
	}

	public String getType()
	{
		return type;
	}

	public String getEventId()
	{
		return eventId;
	}
}
